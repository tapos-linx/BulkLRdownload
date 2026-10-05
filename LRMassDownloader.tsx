import React, { useState, useEffect, useMemo } from 'react';
import {
  Download,
  FolderArchive,
  Pause,
  Play,
  RotateCcw,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Clock,
  Layers,
  MapPin,
  FileText,
  Search,
  CheckSquare,
  Square,
  ShieldCheck,
  ChevronDown
} from 'lucide-react';
import { GeoDataService } from './geoDataService';
import { useDownloadQueue } from './useDownloadQueue';
import { District, Upazila, Mouza, SurveyType } from './types';

const SURVEY_TYPES: SurveyType[] = ['CS', 'SA', 'RS', 'BRS'];

export const LRMassDownloader: React.FC = () => {
  // Cascading Selection State
  const districts = useMemo(() => GeoDataService.getDistricts(), []);
  const [selectedDistrict, setSelectedDistrict] = useState<District>(districts[0]);
  
  const upazilas = useMemo(() => {
    return GeoDataService.getUpazilasByDistrict(selectedDistrict.id);
  }, [selectedDistrict]);

  const [selectedUpazila, setSelectedUpazila] = useState<Upazila>(upazilas[0]);

  // Mouzas in selected Upazila
  const upazilaMouzas = useMemo(() => {
    return GeoDataService.getMouzasByUpazila(selectedUpazila.id);
  }, [selectedUpazila]);

  // Selected Mouza IDs (Default to 100% auto-selected upon Upazila change)
  const [selectedMouzaIds, setSelectedMouzaIds] = useState<Set<string>>(new Set());

  // Survey Series Multi-Selection
  const [selectedSurveys, setSelectedSurveys] = useState<Set<SurveyType>>(
    new Set(['RS', 'BRS'])
  );

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState('');

  // Dropdown open states
  const [districtDropdownOpen, setDistrictDropdownOpen] = useState(false);
  const [upazilaDropdownOpen, setUpazilaDropdownOpen] = useState(false);

  // Download Queue Engine
  const {
    tasks,
    stats,
    isQueueRunning,
    isPaused,
    isPackagingZip,
    zipProgress,
    enqueueMouzas,
    pauseQueue,
    resumeQueue,
    abortQueue,
    retryFailedTasks,
    exportMasterZip
  } = useDownloadQueue();

  // Cascade Rule: When District changes, auto-select first Upazila
  const handleDistrictChange = (dist: District) => {
    setSelectedDistrict(dist);
    setDistrictDropdownOpen(false);
    const newUpazilas = GeoDataService.getUpazilasByDistrict(dist.id);
    if (newUpazilas.length > 0) {
      setSelectedUpazila(newUpazilas[0]);
    }
  };

  // Crucial Automation: As soon as an Upazila is picked, AUTOMATICALLY SELECT ALL MOUZAS (100%)
  useEffect(() => {
    const allIds = new Set(upazilaMouzas.map(m => m.id));
    setSelectedMouzaIds(allIds);
  }, [upazilaMouzas]);

  // Master Select All / Deselect All Toggle
  const toggleSelectAll = () => {
    if (selectedMouzaIds.size === upazilaMouzas.length) {
      setSelectedMouzaIds(new Set());
    } else {
      setSelectedMouzaIds(new Set(upazilaMouzas.map(m => m.id)));
    }
  };

  // Individual Mouza Chip Toggle
  const toggleMouza = (id: string) => {
    const next = new Set(selectedMouzaIds);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    setSelectedMouzaIds(next);
  };

  // Toggle Survey Type
  const toggleSurvey = (survey: SurveyType) => {
    const next = new Set(selectedSurveys);
    if (next.has(survey)) {
      if (next.size > 1) next.delete(survey);
    } else {
      next.add(survey);
    }
    setSelectedSurveys(next);
  };

  // Filtered Mouzas by Search
  const filteredMouzas = useMemo(() => {
    if (!searchQuery.trim()) return upazilaMouzas;
    const query = searchQuery.toLowerCase().trim();
    return upazilaMouzas.filter(
      m =>
        m.nameBn.includes(query) ||
        (m.nameEn && m.nameEn.toLowerCase().includes(query)) ||
        m.jlNo.toLowerCase().includes(query)
    );
  }, [upazilaMouzas, searchQuery]);

  // Start Mass Download Queue
  const handleStartQueue = () => {
    const chosenMouzas = upazilaMouzas.filter(m => selectedMouzaIds.has(m.id));
    if (chosenMouzas.length === 0) return;

    enqueueMouzas(
      chosenMouzas,
      Array.from(selectedSurveys),
      selectedDistrict.nameEn,
      selectedUpazila.nameEn
    );
  };

  return (
    <div className="min-h-screen bg-[#090d16] text-[#f8fafc] font-sans pb-28 select-none">
      {/* Top Luxury Gold Navigation Bar */}
      <header className="sticky top-0 z-40 bg-[#0f1422]/95 backdrop-blur-md border-b border-[#2e2617] px-4 py-3.5 shadow-xl shadow-black/40">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#d4af37] to-[#b38914] p-0.5 shadow-md shadow-amber-900/40 flex items-center justify-center">
              <div className="w-full h-full bg-[#090d16] rounded-[10px] flex items-center justify-center">
                <FolderArchive className="w-5 h-5 text-[#d4af37]" />
              </div>
            </div>
            <div>
              <h1 className="text-lg font-black tracking-tight text-[#fef3c7] leading-tight">
                LR Mass Downloader
              </h1>
              <p className="text-[11px] font-semibold text-[#d4af37] tracking-wider uppercase">
                Offline Land Record Packaging Engine
              </p>
            </div>
          </div>

          <div className="hidden sm:flex items-center space-x-2 bg-[#141b2c] border border-[#2e2617] rounded-xl px-3 py-1.5 text-xs text-[#94a3b8]">
            <ShieldCheck className="w-4 h-4 text-[#d4af37]" />
            <span>Concurrency Limit: 2 Threads</span>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        {/* Module 1: Cascading Location Selector */}
        <section className="bg-[#0f1422] border border-[#2e2617] rounded-3xl p-5 shadow-2xl shadow-black/60 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-48 h-48 bg-[#d4af37]/5 rounded-full blur-3xl pointer-events-none" />

          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <MapPin className="w-5 h-5 text-[#d4af37]" />
              <h2 className="text-sm font-bold uppercase tracking-widest text-[#d4af37]">
                Target Archive Geography / ভৌগোলিক নির্বাচন
              </h2>
            </div>
            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-[#2a200a] text-[#fef08a] border border-[#d4af37]/40">
              Zero Missing Mouzas
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* District Selector */}
            <div className="relative">
              <button
                type="button"
                onClick={() => {
                  setDistrictDropdownOpen(!districtDropdownOpen);
                  setUpazilaDropdownOpen(false);
                }}
                className="w-full h-16 bg-[#141b2c] border border-[#3d321d] hover:border-[#d4af37] rounded-2xl px-4 flex items-center justify-between text-left transition-all duration-200 active:scale-[0.99] focus:outline-none focus:ring-2 focus:ring-[#d4af37]/50 shadow-md"
              >
                <div>
                  <span className="block text-xs font-semibold uppercase tracking-wider text-[#f59e0b]">
                    District / জেলা
                  </span>
                  <span className="text-base sm:text-lg font-bold text-[#fef3c7]">
                    {selectedDistrict.nameEn} ({selectedDistrict.nameBn})
                  </span>
                </div>
                <ChevronDown className="w-5 h-5 text-[#d4af37]" />
              </button>

              {districtDropdownOpen && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-[#141b2c] border border-[#3d321d] rounded-2xl p-2 shadow-2xl z-30 space-y-1">
                  {districts.map(d => (
                    <button
                      key={d.id}
                      type="button"
                      onClick={() => handleDistrictChange(d)}
                      className={`w-full text-left px-4 py-3 rounded-xl transition font-medium ${
                        selectedDistrict.id === d.id
                          ? 'bg-[#2a200a] text-[#fef08a] border border-[#d4af37]'
                          : 'text-[#cbd5e1] hover:bg-[#1a233a]'
                      }`}
                    >
                      <div className="text-base font-bold">{d.nameEn}</div>
                      <div className="text-xs text-[#94a3b8]">{d.nameBn}</div>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Upazila Selector */}
            <div className="relative">
              <button
                type="button"
                onClick={() => {
                  setUpazilaDropdownOpen(!upazilaDropdownOpen);
                  setDistrictDropdownOpen(false);
                }}
                className="w-full h-16 bg-[#141b2c] border border-[#3d321d] hover:border-[#d4af37] rounded-2xl px-4 flex items-center justify-between text-left transition-all duration-200 active:scale-[0.99] focus:outline-none focus:ring-2 focus:ring-[#d4af37]/50 shadow-md"
              >
                <div>
                  <span className="block text-xs font-semibold uppercase tracking-wider text-[#f59e0b]">
                    Upazila / উপজেলা ({upazilas.length})
                  </span>
                  <span className="text-base sm:text-lg font-bold text-[#fef3c7]">
                    {selectedUpazila.nameEn} ({selectedUpazila.nameBn})
                  </span>
                </div>
                <ChevronDown className="w-5 h-5 text-[#d4af37]" />
              </button>

              {upazilaDropdownOpen && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-[#141b2c] border border-[#3d321d] rounded-2xl p-2 shadow-2xl z-30 max-h-64 overflow-y-auto space-y-1">
                  {upazilas.map(u => (
                    <button
                      key={u.id}
                      type="button"
                      onClick={() => {
                        setSelectedUpazila(u);
                        setUpazilaDropdownOpen(false);
                      }}
                      className={`w-full text-left px-4 py-2.5 rounded-xl transition font-medium ${
                        selectedUpazila.id === u.id
                          ? 'bg-[#2a200a] text-[#fef08a] border border-[#d4af37]'
                          : 'text-[#cbd5e1] hover:bg-[#1a233a]'
                      }`}
                    >
                      <div className="text-base font-bold">{u.nameEn}</div>
                      <div className="text-xs text-[#94a3b8]">{u.nameBn}</div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Survey Type Selector */}
          <div className="mt-5 pt-4 border-t border-[#261f14]">
            <div className="flex items-center space-x-2 mb-3">
              <Layers className="w-4 h-4 text-[#d4af37]" />
              <span className="text-xs font-semibold uppercase tracking-wider text-[#f59e0b]">
                Survey Record Series / খতিয়ান টাইপ নির্বাচন
              </span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {SURVEY_TYPES.map(st => {
                const active = selectedSurveys.has(st);
                return (
                  <button
                    key={st}
                    type="button"
                    onClick={() => toggleSurvey(st)}
                    className={`h-12 rounded-xl font-extrabold text-sm transition-all duration-150 flex items-center justify-center space-x-2 border ${
                      active
                        ? 'bg-[#2a200a] text-[#fef08a] border-[#d4af37] shadow-md shadow-amber-900/30'
                        : 'bg-[#141a29] text-[#94a3b8] border-[#261f14] hover:bg-[#1b2337]'
                    }`}
                  >
                    <span>{st} Survey</span>
                    {active && <CheckCircle2 className="w-4 h-4 text-[#d4af37]" />}
                  </button>
                );
              })}
            </div>
          </div>
        </section>

        {/* Module 2: Mouza Auto-Selection & Interactive Grid */}
        <section className="bg-[#0f1422] border border-[#2e2617] rounded-3xl p-5 shadow-2xl shadow-black/60 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center space-x-2">
                <FileText className="w-5 h-5 text-[#d4af37]" />
                <h3 className="text-base font-bold text-[#fef3c7]">
                  {selectedUpazila.nameEn} Mouzas / মৌজা সমূহ
                </h3>
              </div>
              <p className="text-xs text-[#d4af37] font-semibold mt-0.5">
                Selected: {selectedMouzaIds.size} / Total: {upazilaMouzas.length} Mouzas
                {selectedMouzaIds.size === upazilaMouzas.length && ' (100% Auto-Selected)'}
              </p>
            </div>

            <div className="flex items-center space-x-2">
              <button
                type="button"
                onClick={toggleSelectAll}
                className="h-10 px-3.5 bg-[#141b2c] border border-[#3d321d] hover:border-[#d4af37] text-xs font-bold text-[#fef3c7] rounded-xl flex items-center space-x-1.5 transition active:scale-95"
              >
                {selectedMouzaIds.size === upazilaMouzas.length ? (
                  <>
                    <CheckSquare className="w-4 h-4 text-[#d4af37]" />
                    <span>Deselect All</span>
                  </>
                ) : (
                  <>
                    <Square className="w-4 h-4 text-[#94a3b8]" />
                    <span>Select All ({upazilaMouzas.length})</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Search Input */}
          <div className="relative">
            <Search className="w-4 h-4 text-[#94a3b8] absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search Mouza by Bengali name, English, or JL number..."
              className="w-full h-11 pl-10 pr-4 bg-[#141b2c] border border-[#2e2617] rounded-xl text-sm text-[#f8fafc] placeholder-[#64748b] focus:outline-none focus:border-[#d4af37]"
            />
          </div>

          {/* Mouza Chips Display */}
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5 max-h-72 overflow-y-auto p-1">
            {filteredMouzas.map(m => {
              const isSelected = selectedMouzaIds.has(m.id);
              return (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => toggleMouza(m.id)}
                  className={`p-3 rounded-2xl border text-left transition-all duration-150 flex flex-col justify-between active:scale-[0.98] ${
                    isSelected
                      ? 'bg-[#2a200a] border-[#d4af37] text-[#fef08a] shadow-md shadow-amber-950/40'
                      : 'bg-[#141a29] border-[#261f14] text-[#94a3b8] hover:bg-[#1a2135]'
                  }`}
                >
                  <div className="flex items-start justify-between w-full">
                    <span className="text-sm font-bold truncate leading-snug">
                      {m.nameBn}
                    </span>
                    {isSelected ? (
                      <CheckCircle2 className="w-4 h-4 text-[#d4af37] shrink-0 ml-1" />
                    ) : (
                      <div className="w-4 h-4 rounded-full border border-[#3d321d] shrink-0 ml-1" />
                    )}
                  </div>
                  <div className="flex items-center justify-between mt-2 pt-1.5 border-t border-black/20 w-full text-[11px]">
                    <span className={isSelected ? 'text-[#f59e0b] font-semibold' : 'text-[#64748b]'}>
                      {m.jlNo}
                    </span>
                    <span className="text-[10px] text-[#64748b] truncate max-w-[70px]">
                      {m.nameEn}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>

          {/* Phone Target Storage Structure Path Preview */}
          <div className="bg-[#141b2c] border border-[#2e2617] rounded-2xl p-3.5 flex items-center justify-between text-xs">
            <div className="flex items-center space-x-2 text-[#cbd5e1] overflow-hidden">
              <FolderArchive className="w-4 h-4 text-[#d4af37] shrink-0" />
              <span className="font-mono text-[11px] text-[#fbbf24] truncate">
                /Internal Storage/LR_Records/{selectedDistrict.nameEn}/{selectedUpazila.nameEn}/
              </span>
            </div>
            <span className="text-[10px] text-[#94a3b8] shrink-0 font-medium ml-2">
              Nested Master Folder
            </span>
          </div>

          {/* Action Trigger Button */}
          <button
            type="button"
            onClick={handleStartQueue}
            disabled={selectedMouzaIds.size === 0}
            className="w-full h-14 bg-[#d4af37] hover:bg-[#f59e0b] text-[#090d16] font-extrabold text-base sm:text-lg rounded-2xl shadow-xl shadow-amber-900/40 flex items-center justify-center space-x-2 transition duration-150 active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Download className="w-5 h-5 text-[#090d16]" />
            <span>
              Enqueue {selectedMouzaIds.size * selectedSurveys.size} Records ({selectedMouzaIds.size} Mouzas × {selectedSurveys.size} Surveys)
            </span>
          </button>
        </section>

        {/* Module 3: Concurrency Queue HUD & Phone ZIP Downloader */}
        {tasks.length > 0 && (
          <section className="bg-[#0f1422] border border-[#2e2617] rounded-3xl p-5 shadow-2xl shadow-black/60 space-y-4 animate-in fade-in slide-in-from-bottom-3 duration-300">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-[#fef3c7] flex items-center space-x-2">
                  <span>Download Queue HUD</span>
                  {isQueueRunning && (
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                  )}
                </h3>
                <p className="text-xs text-[#94a3b8]">
                  Workers: {stats.activeThreads} / 2 Active Threads | Completed: {stats.completed} of {stats.total}
                </p>
              </div>

              {/* Queue Controls */}
              <div className="flex items-center space-x-2">
                {isPaused ? (
                  <button
                    type="button"
                    onClick={resumeQueue}
                    className="p-2.5 bg-[#141b2c] border border-[#3d321d] hover:border-[#d4af37] rounded-xl text-[#d4af37]"
                    title="Resume Queue"
                  >
                    <Play className="w-4 h-4" />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={pauseQueue}
                    className="p-2.5 bg-[#141b2c] border border-[#3d321d] hover:border-[#d4af37] rounded-xl text-[#f59e0b]"
                    title="Pause Queue"
                  >
                    <Pause className="w-4 h-4" />
                  </button>
                )}

                {stats.failed > 0 && (
                  <button
                    type="button"
                    onClick={retryFailedTasks}
                    className="p-2.5 bg-[#141b2c] border border-[#3d321d] hover:border-amber-500 rounded-xl text-amber-400"
                    title="Retry Failed Tasks"
                  >
                    <RotateCcw className="w-4 h-4" />
                  </button>
                )}

                <button
                  type="button"
                  onClick={abortQueue}
                  className="p-2.5 bg-[#141b2c] border border-[#3d321d] hover:border-red-500 rounded-xl text-red-400"
                  title="Abort & Clear Queue"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Solid Metallic Gold Progress Bar */}
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-semibold">
                <span className="text-[#d4af37]">{stats.percent}% Processed</span>
                <span className="text-[#94a3b8]">
                  {stats.completed} Done | {stats.pending} Pending | {stats.failed} Failed
                </span>
              </div>
              <div className="w-full h-3.5 bg-[#141a29] rounded-full overflow-hidden border border-[#2e2617]">
                <div
                  className="h-full bg-gradient-to-r from-[#d4af37] to-[#f59e0b] rounded-full transition-all duration-300 shadow-md shadow-amber-900/40"
                  style={{ width: `${stats.percent}%` }}
                />
              </div>
            </div>

            {/* Task List Preview */}
            <div className="max-h-48 overflow-y-auto space-y-2 pr-1">
              {tasks.slice(0, 15).map(task => (
                <div
                  key={task.id}
                  className="p-2.5 bg-[#141b2c] border border-[#261f14] rounded-xl flex items-center justify-between text-xs"
                >
                  <div className="flex items-center space-x-2 truncate">
                    {task.status === 'COMPLETED' && (
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                    )}
                    {task.status === 'DOWNLOADING' && (
                      <div className="w-4 h-4 border-2 border-[#d4af37] border-t-transparent rounded-full animate-spin shrink-0" />
                    )}
                    {task.status === 'PENDING' && (
                      <Clock className="w-4 h-4 text-[#64748b] shrink-0" />
                    )}
                    {task.status === 'FAILED' && (
                      <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
                    )}
                    <span className="font-semibold text-[#f8fafc] truncate">
                      {task.fileName}
                    </span>
                  </div>

                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded-md ${
                      task.status === 'COMPLETED'
                        ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                        : task.status === 'DOWNLOADING'
                        ? 'bg-amber-950 text-amber-300 border border-amber-800'
                        : task.status === 'FAILED'
                        ? 'bg-red-950 text-red-300 border border-red-800'
                        : 'bg-[#1b2337] text-[#94a3b8]'
                    }`}
                  >
                    {task.status}
                  </span>
                </div>
              ))}
              {tasks.length > 15 && (
                <p className="text-center text-[11px] text-[#64748b] pt-1">
                  + {tasks.length - 15} more records in sequential queue
                </p>
              )}
            </div>

            {/* Save Master ZIP Button */}
            <div className="pt-2">
              <button
                type="button"
                onClick={exportMasterZip}
                disabled={stats.completed === 0 || isPackagingZip}
                className="w-full h-14 bg-gradient-to-r from-[#d4af37] to-[#f59e0b] hover:from-[#f59e0b] hover:to-[#d4af37] text-[#090d16] font-extrabold text-base sm:text-lg rounded-2xl shadow-xl shadow-amber-900/40 flex items-center justify-center space-x-2 transition duration-150 active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <FolderArchive className="w-5 h-5 text-[#090d16]" />
                <span>
                  {isPackagingZip
                    ? `Packaging Master ZIP (${zipProgress}%)...`
                    : `Save Master ZIP to Phone (${stats.completed} Records)`}
                </span>
              </button>
            </div>
          </section>
        )}
      </main>
    </div>
  );
};

export default LRMassDownloader;

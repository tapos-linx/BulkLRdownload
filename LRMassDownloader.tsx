// LRMassDownloader.tsx
import React, { useState, useMemo } from 'react';
import { GEO_DATA } from './geoData';
import { useDownloadQueue } from './useDownloadQueue';
import { DownloadTask } from './types';
import { 
  Building2, 
  MapPin, 
  CheckSquare, 
  Square, 
  DownloadCloud, 
  Play, 
  Pause, 
  Trash2, 
  Layers, 
  CheckCircle2, 
  Loader2,
  Search,
  X
} from 'lucide-react';

export default function LRMassDownloader() {
  const [selectedDistrictId, setSelectedDistrictId] = useState<string>('cumilla');
  const [selectedUpazilaId, setSelectedUpazilaId] = useState<string>('');
  const [selectedMouzaIds, setSelectedMouzaIds] = useState<string[]>([]);
  const [surveyType, setSurveyType] = useState<string>('RS');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const { tasks, isProcessing, addTasks, pauseQueue, resumeQueue, clearQueue, stats } = useDownloadQueue();

  const currentDistrict = GEO_DATA.find(d => d.id === selectedDistrictId);
  const currentUpazila = currentDistrict?.upazilas.find(u => u.id === selectedUpazilaId);
  const mouzas = currentUpazila?.mouzas || [];

  // ১. ডিস্ট্রিক্ট পরিবর্তন হলে উপজেলা, মৌজা ও সার্চ রিসেট
  const handleDistrictChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedDistrictId(e.target.value);
    setSelectedUpazilaId('');
    setSelectedMouzaIds([]);
    setSearchQuery('');
  };

  // ২. উপজেলা সিলেক্ট করলেই সমস্ত মৌজা অটো সিলেক্ট হবে (AUTO-SELECT ALL)
  const handleUpazilaChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const upazilaId = e.target.value;
    setSelectedUpazilaId(upazilaId);
    setSearchQuery('');

    const targetUpazila = currentDistrict?.upazilas.find(u => u.id === upazilaId);
    if (targetUpazila && targetUpazila.mouzas.length > 0) {
      // সমস্ত মৌজা স্বয়ংক্রিয়ভাবে অ্যারেতে পুশ
      setSelectedMouzaIds(targetUpazila.mouzas.map(m => m.id));
    } else {
      setSelectedMouzaIds([]);
    }
  };

  // ৩. সার্চ ফিল্টার অনুযায়ী মৌজা তালিকা
  const filteredMouzas = useMemo(() => {
    if (!searchQuery.trim()) return mouzas;
    const query = searchQuery.toLowerCase().trim();
    return mouzas.filter(
      m =>
        m.nameBn.toLowerCase().includes(query) ||
        m.jlNo.toLowerCase().includes(query)
    );
  }, [mouzas, searchQuery]);

  // ইন্ডিভিজুয়াল চেকবক্স টগল
  const toggleMouza = (mouzaId: string) => {
    setSelectedMouzaIds(prev =>
      prev.includes(mouzaId) ? prev.filter(id => id !== mouzaId) : [...prev, mouzaId]
    );
  };

  // মাস্টার সিলেক্ট অল / ডিসিলেক্ট অল (সার্চ থাকলে সার্চ রেজাল্ট অনুযায়ী কার্যকর)
  const toggleSelectAllMouzas = () => {
    const targetSet = searchQuery.trim() ? filteredMouzas : mouzas;
    const allTargetSelected = targetSet.length > 0 && targetSet.every(m => selectedMouzaIds.includes(m.id));

    if (allTargetSelected) {
      const targetIds = new Set(targetSet.map(m => m.id));
      setSelectedMouzaIds(prev => prev.filter(id => !targetIds.has(id)));
    } else {
      const newIds = new Set([...selectedMouzaIds, ...targetSet.map(m => m.id)]);
      setSelectedMouzaIds(Array.from(newIds));
    }
  };

  // কিউ-তে টাস্ক পুশ করা
  const handleQueueDownloads = () => {
    if (!currentDistrict || !currentUpazila || selectedMouzaIds.length === 0) return;

    const newTasks: DownloadTask[] = selectedMouzaIds.map(mouzaId => {
      const mouza = mouzas.find(m => m.id === mouzaId)!;
      return {
        id: `${surveyType}-${mouza.id}-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
        mouzaId: mouza.id,
        mouzaName: mouza.nameBn,
        jlNo: mouza.jlNo,
        upazilaName: currentUpazila.nameBn,
        districtName: currentDistrict.nameBn,
        recordType: surveyType,
        status: 'PENDING',
        progress: 0
      };
    });

    addTasks(newTasks);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 p-4 md:p-8 font-sans">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header */}
        <header className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-slate-200">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-emerald-900 flex items-center gap-2">
              <Layers className="w-7 h-7 text-emerald-600" />
              LR Mass Downloader (কুমিল্লা ও ব্রাহ্মণবাড়িয়া)
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              স্বয়ংক্রিয় মৌজা নির্বাচন এবং কনকারেন্ট কিউ ভিত্তিক বাল্ক ল্যান্ড রেকর্ড ডাউনলোডার
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold px-3 py-1 bg-emerald-100 text-emerald-800 rounded-full">
              সার্ভে: {surveyType}
            </span>
            <span className="text-xs font-semibold px-3 py-1 bg-slate-100 text-slate-700 rounded-full">
              অটো-সিলেক্ট সক্রিয়
            </span>
          </div>
        </header>

        {/* Control Card (Dropdowns & Filters) */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-white p-6 rounded-3xl shadow-sm border border-slate-200">
          
          {/* জেলা নির্বাচন */}
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-600 flex items-center gap-1.5">
              <Building2 className="w-4 h-4 text-emerald-600" /> ১. জেলা নির্বাচন করুন
            </label>
            <select
              value={selectedDistrictId}
              onChange={handleDistrictChange}
              className="w-full bg-slate-50 border border-slate-300 rounded-2xl px-4 py-3 text-sm font-medium focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            >
              {GEO_DATA.map(d => (
                <option key={d.id} value={d.id}>{d.nameBn}</option>
              ))}
            </select>
          </div>

          {/* উপজেলা নির্বাচন */}
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-600 flex items-center gap-1.5">
              <MapPin className="w-4 h-4 text-emerald-600" /> ২. উপজেলা নির্বাচন করুন
            </label>
            <select
              value={selectedUpazilaId}
              onChange={handleUpazilaChange}
              className="w-full bg-slate-50 border border-slate-300 rounded-2xl px-4 py-3 text-sm font-medium focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            >
              <option value="">-- উপজেলা বাছাই করুন --</option>
              {currentDistrict?.upazilas.map(u => (
                <option key={u.id} value={u.id}>{u.nameBn} ({u.mouzas.length} টি মৌজা)</option>
              ))}
            </select>
          </div>

          {/* সার্ভে টাইপ */}
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-600 flex items-center gap-1.5">
              <Layers className="w-4 h-4 text-emerald-600" /> ৩. রেকর্ডের ধরন
            </label>
            <select
              value={surveyType}
              onChange={e => setSurveyType(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-2xl px-4 py-3 text-sm font-medium focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            >
              <option value="CS">সিএস (CS)</option>
              <option value="SA">এসএ (SA)</option>
              <option value="RS">আরএস (RS)</option>
              <option value="BRS">বিআরএস / বিএস (BRS)</option>
            </select>
          </div>
        </div>

        {/* মৌজা তালিকা, সার্চ বার ও অটো-সিলেক্ট ভিউ */}
        {selectedUpazilaId && (
          <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
              <div>
                <h2 className="text-lg font-bold text-slate-800">
                  {currentUpazila?.nameBn} উপজেলার মৌজা তালিকা
                </h2>
                <p className="text-xs text-slate-500">
                  উপজেলা সিলেক্ট করায় সমস্ত মৌজা স্বয়ংক্রিয়ভাবে টিকচিহ্নিত হয়েছে। প্রয়োজনে আনচেক বা সার্চ করে ফিল্টার করুন।
                </p>
              </div>

              <div className="flex items-center gap-3">
                <button
                  onClick={toggleSelectAllMouzas}
                  className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-xl border border-slate-300 hover:bg-slate-100 transition"
                >
                  {filteredMouzas.length > 0 && filteredMouzas.every(m => selectedMouzaIds.includes(m.id)) ? (
                    <><CheckSquare className="w-4 h-4 text-emerald-600" /> {searchQuery ? 'ফিল্টারকৃত বাদ দিন' : 'সব বাদ দিন'}</>
                  ) : (
                    <><Square className="w-4 h-4" /> {searchQuery ? 'ফিল্টারকৃত সিলেক্ট করুন' : 'সব সিলেক্ট করুন'}</>
                  )}
                </button>

                <span className="text-xs font-bold px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-xl border border-emerald-200">
                  সিলেক্টেড: {selectedMouzaIds.length} / {mouzas.length}
                  {searchQuery && ` (ফলাফল: ${filteredMouzas.length})`}
                </span>

                <button
                  onClick={handleQueueDownloads}
                  disabled={selectedMouzaIds.length === 0}
                  className="flex items-center gap-2 px-5 py-2 text-sm font-semibold rounded-2xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 transition shadow-sm"
                >
                  <DownloadCloud className="w-4 h-4" />
                  ডাউনলোড কিউতে যোগ করুন ({selectedMouzaIds.length})
                </button>
              </div>
            </div>

            {/* সার্চ বার (Search Filter) */}
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none">
                <Search className="w-4 h-4 text-slate-400" />
              </div>
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder={`${currentUpazila?.nameBn} এর মৌজার নাম বা জেএল নম্বর (যেমন: ${mouzas[0]?.jlNo || '০১'}) দিয়ে খুঁজুন...`}
                className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-10 pr-10 py-2.5 text-sm font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white transition"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 transition"
                  title="অনুসন্ধান মুছুন"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            {/* মৌজা গ্রিড বা নো-রেজাল্ট স্টেট */}
            {filteredMouzas.length === 0 ? (
              <div className="py-10 text-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/50">
                <Search className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                <p className="text-sm font-semibold text-slate-600">
                  "{searchQuery}" এর সাথে মিলে এমন কোনো মৌজা পাওয়া যায়নি
                </p>
                <p className="text-xs text-slate-400 mt-1">
                  মৌজার বাংলা বানান বা জেএল নম্বর পুনরায় যাচাই করুন
                </p>
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="mt-3 text-xs font-semibold text-emerald-600 hover:underline"
                >
                  অনুসন্ধান রিসেট করুন
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2.5 max-h-72 overflow-y-auto pr-1">
                {filteredMouzas.map(m => {
                  const isSelected = selectedMouzaIds.includes(m.id);
                  return (
                    <div
                      key={m.id}
                      onClick={() => toggleMouza(m.id)}
                      className={`cursor-pointer p-3 rounded-2xl border transition flex items-center justify-between select-none ${
                        isSelected
                          ? 'border-emerald-500 bg-emerald-50/60 text-emerald-950 font-medium'
                          : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <div>
                        <div className="text-sm">{m.nameBn}</div>
                        <div className="text-[11px] text-slate-400">জেএল নং: {m.jlNo}</div>
                      </div>
                      {isSelected ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                      ) : (
                        <Square className="w-4 h-4 text-slate-300 flex-shrink-0" />
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* ডাউনলোড কিউ ও প্রগ্রেস প্যানেল */}
        {tasks.length > 0 && (
          <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h3 className="text-base font-bold text-slate-800 flex items-center gap-2">
                  <DownloadCloud className="w-5 h-5 text-emerald-600" />
                  ডাউনলোড কিউ স্টেটাস (সমগ্র অগ্রগতি: {stats.overallPercent}%)
                </h3>
                <p className="text-xs text-slate-500">
                  মোট: {stats.total} টি | সম্পন্ন: {stats.completed} | ডাউনলোড হচ্ছে: {stats.downloading} | অপেক্ষমাণ: {stats.pending}
                </p>
              </div>

              <div className="flex items-center gap-2">
                {isProcessing ? (
                  <button
                    onClick={pauseQueue}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-xl bg-amber-50 text-amber-800 border border-amber-300 hover:bg-amber-100 transition"
                  >
                    <Pause className="w-3.5 h-3.5" /> পজ করুন
                  </button>
                ) : (
                  <button
                    onClick={resumeQueue}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-xl bg-emerald-50 text-emerald-800 border border-emerald-300 hover:bg-emerald-100 transition"
                  >
                    <Play className="w-3.5 h-3.5" /> পুনরায় শুরু করুন
                  </button>
                )}
                <button
                  onClick={clearQueue}
                  className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-xl bg-rose-50 text-rose-800 border border-rose-200 hover:bg-rose-100 transition"
                >
                  <Trash2 className="w-3.5 h-3.5" /> তালিকা মুছুন
                </button>
              </div>
            </div>

            {/* ওভারঅল প্রগ্রেস বার */}
            <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
              <div
                className="bg-emerald-600 h-2.5 rounded-full transition-all duration-300"
                style={{ width: `${stats.overallPercent}%` }}
              />
            </div>

            {/* লাইভ টাস্ক তালিকা */}
            <div className="divide-y divide-slate-100 max-h-60 overflow-y-auto">
              {tasks.map(t => (
                <div key={t.id} className="py-2.5 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-slate-800">{t.mouzaName}</span>
                    <span className="text-slate-400">(জেএল: {t.jlNo})</span>
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 text-[10px]">
                      {t.upazilaName}, {t.districtName} [{t.recordType}]
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    {t.status === 'PENDING' && (
                      <span className="text-slate-400 font-medium">অপেক্ষমাণ...</span>
                    )}
                    {t.status === 'DOWNLOADING' && (
                      <span className="flex items-center gap-1 text-emerald-600 font-semibold">
                        <Loader2 className="w-3.5 h-3.5 animate-spin" /> {t.progress}%
                      </span>
                    )}
                    {t.status === 'COMPLETED' && (
                      <span className="flex items-center gap-1 text-emerald-700 font-semibold">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> সম্পন্ন
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

      </div>
    </div>
  );
}

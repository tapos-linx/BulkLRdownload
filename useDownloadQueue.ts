// useDownloadQueue.ts
import { useState, useEffect, useRef } from 'react';
import { DownloadTask } from './types';

const MAX_CONCURRENT_DOWNLOADS = 2;

export function useDownloadQueue() {
  const [tasks, setTasks] = useState<DownloadTask[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const isPausedRef = useRef(false);

  const addTasks = (newTasks: DownloadTask[]) => {
    setTasks(prev => [...prev, ...newTasks]);
    setIsProcessing(true);
    isPausedRef.current = false;
  };

  const pauseQueue = () => {
    isPausedRef.current = true;
    setIsProcessing(false);
  };

  const resumeQueue = () => {
    isPausedRef.current = false;
    setIsProcessing(true);
  };

  const clearQueue = () => {
    isPausedRef.current = true;
    setTasks([]);
    setIsProcessing(false);
  };

  // Queue runner
  useEffect(() => {
    if (!isProcessing) return;

    const interval = setInterval(() => {
      if (isPausedRef.current) return;

      setTasks(currentTasks => {
        const activeCount = currentTasks.filter(t => t.status === 'DOWNLOADING').length;
        if (activeCount >= MAX_CONCURRENT_DOWNLOADS) return currentTasks;

        const nextPendingIndex = currentTasks.findIndex(t => t.status === 'PENDING');
        if (nextPendingIndex === -1) {
          if (activeCount === 0) setIsProcessing(false);
          return currentTasks;
        }

        const updated = [...currentTasks];
        const taskToStart = { ...updated[nextPendingIndex], status: 'DOWNLOADING' as const };
        updated[nextPendingIndex] = taskToStart;

        // Mock download executor (simulate fetch + blob download)
        executeDownload(taskToStart.id);

        return updated;
      });
    }, 600);

    return () => clearInterval(interval);
  }, [isProcessing]);

  const executeDownload = (taskId: string) => {
    let progress = 0;
    const progressInterval = setInterval(() => {
      progress += 25;
      setTasks(prev =>
        prev.map(t => (t.id === taskId ? { ...t, progress: Math.min(progress, 100) } : t))
      );

      if (progress >= 100) {
        clearInterval(progressInterval);
        setTasks(prev =>
          prev.map(t =>
            t.id === taskId ? { ...t, status: 'COMPLETED', progress: 100 } : t
          )
        );
      }
    }, 350);
  };

  const stats = {
    total: tasks.length,
    completed: tasks.filter(t => t.status === 'COMPLETED').length,
    downloading: tasks.filter(t => t.status === 'DOWNLOADING').length,
    pending: tasks.filter(t => t.status === 'PENDING').length,
    failed: tasks.filter(t => t.status === 'FAILED').length,
    overallPercent: tasks.length === 0 ? 0 : Math.round((tasks.filter(t => t.status === 'COMPLETED').length / tasks.length) * 100)
  };

  return {
    tasks,
    isProcessing,
    addTasks,
    pauseQueue,
    resumeQueue,
    clearQueue,
    stats
  };
}

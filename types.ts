// types.ts
export interface Mouza {
  id: string;
  nameBn: string;
  jlNo: string;
}

export interface Upazila {
  id: string;
  nameBn: string;
  mouzas: Mouza[];
}

export interface District {
  id: string;
  nameBn: string;
  upazilas: Upazila[];
}

export type TaskStatus = 'PENDING' | 'DOWNLOADING' | 'COMPLETED' | 'FAILED';

export interface DownloadTask {
  id: string;
  mouzaId: string;
  mouzaName: string;
  jlNo: string;
  upazilaName: string;
  districtName: string;
  recordType: string;
  status: TaskStatus;
  progress: number;
  error?: string;
}

export interface QueueStats {
  total: number;
  pending: number;
  downloading: number;
  completed: number;
  failed: number;
  percent: number;
  activeThreads: number;
}

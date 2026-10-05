/**
 * Land Record (LR) Mass Downloader - Core Data Models & Types
 * Repository: tapos-linx/BulkLRdownload
 */

export type SurveyType = 'CS' | 'SA' | 'RS' | 'BRS';

export interface District {
  id: string;
  nameEn: string;
  nameBn: string;
}

export interface Upazila {
  id: string;
  nameEn: string;
  nameBn: string;
  districtId: string;
}

export interface Mouza {
  id: string;
  nameBn: string;
  jlNo: string;
  upazilaId: string;
  districtId: string;
  nameEn?: string;
}

export type TaskStatus = 'PENDING' | 'DOWNLOADING' | 'COMPLETED' | 'FAILED';

export interface DownloadTask {
  id: string;
  mouzaId: string;
  mouzaName: string;
  jlNo: string;
  districtId: string;
  districtName: string;
  upazilaId: string;
  upazilaName: string;
  surveyType: SurveyType;
  fileName: string;
  targetPath: string;
  status: TaskStatus;
  progress: number;
  retryCount: number;
  maxRetries: number;
  error?: string;
  blob?: Blob;
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

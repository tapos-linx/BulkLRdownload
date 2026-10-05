import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.tapos.bulklrdownload',
  appName: 'LR Mass Downloader',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
    cleartext: true
  },
  android: {
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: false,
    backgroundColor: '#ffffff'
  },
  plugins: {
    Filesystem: {
      // Configuration for file storage and directory access
    }
  }
};

export default config;

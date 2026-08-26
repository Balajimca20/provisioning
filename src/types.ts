export type TabType = 'dashboard' | 'wifi' | 'adb_setup' | 'cmd_ota' | 'ota' | 'supplier' | 'terminal';

export interface DeviceConnectionState {
  isWifiConnected: boolean;
  connectedSsid: string;
  isAdbConnected: boolean;
  adbHost: string;
  adbPort: string;
  batteryVoltage: string;
  ecuStatus: string;
  storageSpace: string;
  activeSlot: 'A' | 'B';
  buildVariant: 'DEV' | 'UAT' | 'PROD';
  serialNumber: string;
}

export interface TerminalLogLine {
  id: string;
  text: string;
  type?: 'info' | 'success' | 'warning' | 'error' | 'command';
  timestamp: string;
}

export interface OtaPackageInfo {
  fileName: string;
  fileSizeBytes: number;
  fileSizeFormatted: string;
  md5Checksum: string;
  targetVersion: string;
}

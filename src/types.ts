export type TabType = 'dashboard' | 'wifi' | 'ota' | 'supplier' | 'terminal';

export type EnvironmentType = 'dev' | 'uat' | 'prod';

export type WifiConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export interface WifiState {
  status: WifiConnectionStatus;
  ssid: string;
  password?: string;
  ipAddress?: string;
  gateway?: string;
  signalStrength?: number; // dBm e.g. -45
  connectedAt?: string;
  error?: string;
}

export type AdbConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export interface AdbState {
  status: AdbConnectionStatus;
  host: string;
  port: number;
  isRoot: boolean;
  deviceModel?: string;
  serialNumber?: string;
  androidVersion?: string;
  connectedAt?: string;
  error?: string;
}

export interface SoftApConfig {
  ssid: string;
  passphrase: string;
  securityType: 'WPA2_PSK' | 'WPA3_SAE' | 'NONE';
  band: '2.4GHz' | '5GHz' | 'DUAL';
  channel: number;
  hiddenSsid: boolean;
  maxConnections: number;
  rawXml: string;
  lastUpdated: string;
}

export interface WifiLogRecord {
  id: string;
  timestamp: string;
  oldSsid: string;
  newSsid: string;
  oldPasswordMasked: string;
  newPasswordMasked: string;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  details: string;
}

export type OtaStepType = 'idle' | 'precheck' | 'download' | 'push' | 'verify' | 'flash' | 'awaiting_reboot' | 'rebooting' | 'postcheck' | 'done' | 'failed';

export interface OtaPackage {
  id: string;
  version: string;
  buildNumber: string;
  targetModel: string;
  sizeBytes: number;
  sizeFormatted: string;
  checksumSha256: string;
  releaseNotes: string[];
  mandatory: boolean;
}

export interface OtaLogEntry {
  id: string;
  timestamp: string;
  level: 'info' | 'warn' | 'error' | 'success';
  message: string;
}

export interface OtaState {
  status: OtaStepType;
  selectedPackage: OtaPackage | null;
  progress: number; // 0 - 100
  stepDescription: string;
  logs: OtaLogEntry[];
  startTime?: string;
  completedAt?: string;
  error?: string;
}

export interface DeviceTelemetry {
  serialNumber: string;
  vin: string;
  model: string;
  manufactureDate: string;
  firmwareVersion: string;
  hardwareRevision: string;
  ecuStatus: 'OK' | 'WARNING' | 'FAULT';
  telematicsImei: string;
  telematicsIccid: string;
  batteryVoltage: number;
  batterySoC: number;
  engineTemp: number;
  odometerKm: number;
  lastSynced: string;
  supplierName: string;
  warrantyStatus: 'ACTIVE' | 'EXPIRED' | 'CLAIM_PENDING';
  fields: {
    category: string;
    items: { key: string; label: string; value: string; status: 'normal' | 'warn' | 'alert' }[];
  }[];
}

export type SupplierBannerState = 'idle' | 'loading' | 'loaded' | 'not_found' | 'error';

export interface NetworkDiagnosticProbe {
  target: string;
  type: 'ADB_TCP' | 'SOFTAP_GATEWAY' | 'SUPPLIER_GRAPHQL' | 'DNS_DHCP';
  latencyMs: number;
  status: 'ONLINE' | 'OFFLINE' | 'DEGRADED' | 'CHECKING';
  details: string;
  lastChecked: string;
}

export interface LiveGraphQLMeta {
  endpoint: string;
  latencyMs: number;
  httpStatus: number;
  timestamp: string;
  isRealtime: boolean;
  querySize: number;
}

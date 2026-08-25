import { SoftApConfig, WifiLogRecord } from '../types';

const STORAGE_CONFIG_KEY = 're_ff_softap_config';
const STORAGE_LOGS_KEY = 're_ff_wifi_changelogs';

export function generateSoftApXml(config: Partial<SoftApConfig>): string {
  const ssid = config.ssid || 'RE_LXHD_250925';
  const passphrase = config.passphrase || 'RoyalEnfield@2026';
  const sec = config.securityType || 'WPA2_PSK';
  const channel = config.channel || 6;
  const band = config.band || '2.4GHz';
  const hidden = config.hiddenSsid ?? false;
  const maxConn = config.maxConnections || 8;

  return `<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<WifiConfigStoreData>
  <Version>3</Version>
  <SoftApConfiguration>
    <SSID>${ssid}</SSID>
    <PreSharedKey>${passphrase}</PreSharedKey>
    <SecurityType>${sec}</SecurityType>
    <Channel>${channel}</Channel>
    <Band>${band}</Band>
    <HiddenSSID>${hidden}</HiddenSSID>
    <MaxScannedClientCount>${maxConn}</MaxScannedClientCount>
    <MacRandomizationSetting>0</MacRandomizationSetting>
    <LastModifiedBy>com.royalenfield.provisioning</LastModifiedBy>
    <Timestamp>${new Date().toISOString()}</Timestamp>
  </SoftApConfiguration>
</WifiConfigStoreData>`;
}

export function parseSoftApXml(xml: string): Partial<SoftApConfig> {
  const getTag = (tag: string) => {
    const match = xml.match(new RegExp(`<${tag}>(.*?)</${tag}>`, 'i'));
    return match ? match[1] : '';
  };

  const ssid = getTag('SSID') || 'RE_LXHD_250925';
  const passphrase = getTag('PreSharedKey') || 'RoyalEnfield@2026';
  const securityType = (getTag('SecurityType') || 'WPA2_PSK') as SoftApConfig['securityType'];
  const band = (getTag('Band') || '2.4GHz') as SoftApConfig['band'];
  const channel = parseInt(getTag('Channel') || '6', 10);
  const hiddenSsid = getTag('HiddenSSID') === 'true';
  const maxConnections = parseInt(getTag('MaxScannedClientCount') || '8', 10);

  return {
    ssid,
    passphrase,
    securityType,
    band,
    channel,
    hiddenSsid,
    maxConnections,
    rawXml: xml,
    lastUpdated: new Date().toISOString()
  };
}

export function loadSavedSoftApConfig(): SoftApConfig {
  const saved = localStorage.getItem(STORAGE_CONFIG_KEY);
  if (saved) {
    try {
      return JSON.parse(saved);
    } catch {
      // fallback
    }
  }

  const initial: SoftApConfig = {
    ssid: 'RE_LXHD_250925',
    passphrase: 'RoyalEnfield@2026',
    securityType: 'WPA2_PSK',
    band: '2.4GHz',
    channel: 6,
    hiddenSsid: false,
    maxConnections: 8,
    rawXml: '',
    lastUpdated: new Date().toISOString()
  };
  initial.rawXml = generateSoftApXml(initial);
  return initial;
}

export function saveSoftApConfig(config: SoftApConfig): void {
  config.rawXml = generateSoftApXml(config);
  config.lastUpdated = new Date().toISOString();
  localStorage.setItem(STORAGE_CONFIG_KEY, JSON.stringify(config));
}

export function loadWifiChangeLogs(): WifiLogRecord[] {
  const saved = localStorage.getItem(STORAGE_LOGS_KEY);
  if (saved) {
    try {
      return JSON.parse(saved);
    } catch {
      // fallback
    }
  }

  const sampleLogs: WifiLogRecord[] = [
    {
      id: 'log-001',
      timestamp: '2026-08-24 18:42:10',
      oldSsid: 'RE_FACTORY_000000',
      newSsid: 'RE_LXHD_250925',
      oldPasswordMasked: '••••••••',
      newPasswordMasked: 'RoyalEn••••••',
      status: 'SUCCESS',
      details: 'Pushed /data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml (chmod 600) & rebooted'
    },
    {
      id: 'log-002',
      timestamp: '2026-08-23 11:15:33',
      oldSsid: 'RE_PROV_DEFAULT1',
      newSsid: 'RE_PROV_DEFAULT1',
      oldPasswordMasked: '••••••••',
      newPasswordMasked: 'Enfield@••••',
      status: 'SUCCESS',
      details: 'Passphrase rotation applied during PDI checklist'
    }
  ];
  return sampleLogs;
}

let wifiLogCounter = 0;
export function appendWifiChangeLog(log: Omit<WifiLogRecord, 'id' | 'timestamp'>): WifiLogRecord {
  const current = loadWifiChangeLogs();
  const newRecord: WifiLogRecord = {
    id: `wifi-log-${Date.now()}-${++wifiLogCounter}-${Math.random().toString(36).substring(2, 7)}`,
    timestamp: new Date().toLocaleString(),
    ...log
  };
  const updated = [newRecord, ...current].slice(0, 50);
  localStorage.setItem(STORAGE_LOGS_KEY, JSON.stringify(updated));
  return newRecord;
}

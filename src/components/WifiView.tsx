import React, { useState } from 'react';
import { Radio, RefreshCw, CheckCircle2, AlertCircle, FileCode, History, ShieldCheck, Power, Copy, Check } from 'lucide-react';
import { AdbState, SoftApConfig, WifiLogRecord, WifiState } from '../types';
import { SsidValidator } from '../lib/ssidValidator';
import { RealtimeAdbClient } from '../lib/realtimeAdbClient';
import { appendWifiChangeLog, generateSoftApXml, loadSavedSoftApConfig, saveSoftApConfig } from '../lib/wifiStore';

interface WifiViewProps {
  wifiState: WifiState;
  adbState: AdbState;
  changeLogs: WifiLogRecord[];
  setChangeLogs: React.Dispatch<React.SetStateAction<WifiLogRecord[]>>;
  onQuickReboot: () => void;
}

export const WifiView: React.FC<WifiViewProps> = ({
  adbState,
  changeLogs,
  setChangeLogs,
  onQuickReboot,
}) => {
  const [config, setConfig] = useState<SoftApConfig>(() => loadSavedSoftApConfig());
  const [newSsid, setNewSsid] = useState(config.ssid || 'RE_LXHD_250925');
  const [newPassphrase, setNewPassphrase] = useState(config.passphrase || 'RoyalEnfield@2026');
  const [securityType, setSecurityType] = useState<SoftApConfig['securityType']>(config.securityType || 'WPA2_PSK');
  const [band, setBand] = useState<SoftApConfig['band']>(config.band || '2.4GHz');
  const [channel, setChannel] = useState<number>(config.channel || 6);

  const [isUpdating, setIsUpdating] = useState(false);
  const [updateLogs, setUpdateLogs] = useState<string[]>([]);
  const [progressPercent, setProgressPercent] = useState(0);
  const [updateSuccess, setUpdateSuccess] = useState(false);
  const [copiedXml, setCopiedXml] = useState(false);

  const ssidError = SsidValidator.getValidationError(newSsid);
  const isSsidValid = ssidError === null;

  const handleApplyUpdate = async () => {
    if (!isSsidValid || !newPassphrase) return;

    setIsUpdating(true);
    setUpdateSuccess(false);
    setUpdateLogs([]);
    setProgressPercent(0);

    const oldSsid = config.ssid;

    const adbEngine = RealtimeAdbClient.getInstance();
    const generator = adbEngine.runWifiUpdateWorkflow(newSsid, newPassphrase);

    for await (const step of generator) {
      setUpdateLogs((prev) => [...prev, step.log]);
      setProgressPercent(step.percent);
      if (step.done) {
        setUpdateSuccess(true);
        const updatedConfig: SoftApConfig = {
          ...config,
          ssid: newSsid,
          passphrase: newPassphrase,
          securityType,
          band,
          channel,
          rawXml: generateSoftApXml({
            ssid: newSsid,
            passphrase: newPassphrase,
            securityType,
            band,
            channel,
          }),
          lastUpdated: new Date().toISOString(),
        };
        setConfig(updatedConfig);
        saveSoftApConfig(updatedConfig);

        const newLog = appendWifiChangeLog({
          oldSsid,
          newSsid,
          oldPasswordMasked: '••••••••',
          newPasswordMasked: newPassphrase.substring(0, 4) + '••••••••',
          status: 'SUCCESS',
          details: `SoftAP XML updated: ${newSsid} (${securityType}, ${band} Ch ${channel}) & chmod 600 applied.`,
        });
        setChangeLogs((prev) => [newLog, ...prev]);
      }
    }

    setIsUpdating(false);
  };

  const handleCopyXml = () => {
    navigator.clipboard.writeText(config.rawXml);
    setCopiedXml(true);
    setTimeout(() => setCopiedXml(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Title & Info Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-stone-900 border border-stone-800 rounded-xl p-5">
        <div>
          <div className="flex items-center gap-2">
            <Radio className="w-5 h-5 text-red-500" />
            <h2 className="text-lg font-bold text-white">Vehicle Wi-Fi SoftAP Configuration</h2>
          </div>
          <p className="text-xs text-stone-400 mt-1 font-mono">
            Target XML: /data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml
          </p>
        </div>
        <div className="flex items-center gap-2">
          {adbState.status !== 'connected' && (
            <span className="text-xs px-2.5 py-1 bg-amber-950/70 border border-amber-800/60 text-amber-300 rounded font-medium flex items-center gap-1.5">
              <AlertCircle className="w-3.5 h-3.5" />
              <span>ADB Offline (Simulated Mode)</span>
            </span>
          )}
          <button
            onClick={onQuickReboot}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-stone-800 hover:bg-stone-700 border border-stone-700 text-stone-200 text-xs font-semibold rounded-lg transition-colors cursor-pointer"
          >
            <Power className="w-3.5 h-3.5 text-red-400" />
            <span>Reboot Cluster</span>
          </button>
        </div>
      </div>

      {/* Main 2-Column Editor Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Form & Update Workflow (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          <div className="bg-stone-900 border border-stone-800 rounded-xl p-6 space-y-5">
            <h3 className="text-sm font-bold uppercase tracking-wider text-stone-200 border-b border-stone-800 pb-3 flex items-center justify-between">
              <span>Modify SoftAP Parameters</span>
              <span className="text-xs text-stone-500 font-mono">Schema v3</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* SSID Field */}
              <div className="sm:col-span-2">
                <div className="flex justify-between items-center mb-1">
                  <label className="text-xs font-semibold text-stone-300">
                    New SSID (RE_XXXX_XXXXXX)
                  </label>
                  <span className="text-[11px] font-mono text-stone-500">14 characters</span>
                </div>
                <input
                  type="text"
                  value={newSsid}
                  onChange={(e) => setNewSsid(e.target.value.toUpperCase())}
                  className={`w-full bg-stone-950 border px-3 py-2 rounded-lg text-sm font-mono text-white placeholder-stone-600 focus:outline-none ${
                    newSsid.length > 0 && !isSsidValid ? 'border-red-500' : 'border-stone-700 focus:border-red-500'
                  }`}
                />
                {newSsid.length > 0 && ssidError && (
                  <p className="text-xs text-red-400 mt-1">{ssidError}</p>
                )}
              </div>

              {/* Passphrase Field */}
              <div className="sm:col-span-2">
                <label className="block text-xs font-semibold text-stone-300 mb-1">
                  Pre-Shared Key (Passphrase)
                </label>
                <input
                  type="text"
                  value={newPassphrase}
                  onChange={(e) => setNewPassphrase(e.target.value)}
                  className="w-full bg-stone-950 border border-stone-700 px-3 py-2 rounded-lg text-sm font-mono text-white placeholder-stone-600 focus:outline-none focus:border-red-500"
                />
              </div>

              {/* Security Type */}
              <div>
                <label className="block text-xs font-semibold text-stone-300 mb-1">
                  Security Protocol
                </label>
                <select
                  value={securityType}
                  onChange={(e) => setSecurityType(e.target.value as SoftApConfig['securityType'])}
                  className="w-full bg-stone-950 border border-stone-700 px-3 py-2 rounded-lg text-sm font-mono text-white focus:outline-none focus:border-red-500"
                >
                  <option value="WPA2_PSK">WPA2 PSK (Standard)</option>
                  <option value="WPA3_SAE">WPA3 SAE (Enhanced)</option>
                  <option value="NONE">Open / None</option>
                </select>
              </div>

              {/* Wi-Fi Band */}
              <div>
                <label className="block text-xs font-semibold text-stone-300 mb-1">
                  Radio Band
                </label>
                <select
                  value={band}
                  onChange={(e) => setBand(e.target.value as SoftApConfig['band'])}
                  className="w-full bg-stone-950 border border-stone-700 px-3 py-2 rounded-lg text-sm font-mono text-white focus:outline-none focus:border-red-500"
                >
                  <option value="2.4GHz">2.4 GHz (Standard Reach)</option>
                  <option value="5GHz">5.0 GHz (High Throughput)</option>
                  <option value="DUAL">Dual Concurrent</option>
                </select>
              </div>

              {/* Wi-Fi Channel */}
              <div>
                <label className="block text-xs font-semibold text-stone-300 mb-1">
                  Operating Channel
                </label>
                <select
                  value={channel}
                  onChange={(e) => setChannel(parseInt(e.target.value, 10))}
                  className="w-full bg-stone-950 border border-stone-700 px-3 py-2 rounded-lg text-sm font-mono text-white focus:outline-none focus:border-red-500"
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map((ch) => (
                    <option key={ch} value={ch}>
                      Channel {ch} {ch === 6 ? '(Default Automotive)' : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Max Client Limit */}
              <div>
                <label className="block text-xs font-semibold text-stone-300 mb-1">
                  Max Client Scanned Limit
                </label>
                <input
                  type="number"
                  readOnly
                  value={8}
                  className="w-full bg-stone-950/60 border border-stone-800 px-3 py-2 rounded-lg text-sm font-mono text-stone-400 cursor-not-allowed"
                />
              </div>
            </div>

            {/* Apply Action Button */}
            <button
              onClick={handleApplyUpdate}
              disabled={!isSsidValid || isUpdating}
              className={`w-full py-3 px-4 rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-lg transition-all cursor-pointer ${
                isSsidValid && !isUpdating
                  ? 'bg-red-600 hover:bg-red-500 text-white shadow-red-900/40'
                  : 'bg-stone-800 text-stone-500 cursor-not-allowed'
              }`}
            >
              {isUpdating ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Executing Workflow ({progressPercent}%)...</span>
                </>
              ) : (
                <>
                  <Radio className="w-4 h-4" />
                  <span>Push & Update SoftAP XML via ADB</span>
                </>
              )}
            </button>
          </div>

          {/* Workflow Execution Console */}
          {(isUpdating || updateLogs.length > 0) && (
            <div className="bg-stone-950 border border-stone-800 rounded-xl p-5 space-y-3 font-mono text-xs">
              <div className="flex items-center justify-between">
                <span className="font-bold text-stone-300 flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-cyan-400" />
                  <span>ADB Execution Progress</span>
                </span>
                <span className="text-cyan-400 font-bold">{progressPercent}%</span>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-stone-800 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-gradient-to-r from-red-600 to-cyan-500 h-2 transition-all duration-300 rounded-full"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>

              {/* Step Logs */}
              <div className="bg-black/50 p-3 rounded-lg border border-stone-900 space-y-1.5 max-h-40 overflow-y-auto">
                {updateLogs.map((log, i) => (
                  <div
                    key={i}
                    className={`flex items-start gap-2 ${
                      log.includes('[SUCCESS]')
                        ? 'text-emerald-400 font-bold'
                        : log.includes('[ERROR]')
                        ? 'text-red-400'
                        : 'text-stone-400'
                    }`}
                  >
                    <span className="text-stone-600 select-none">&gt;</span>
                    <span>{log}</span>
                  </div>
                ))}
              </div>

              {updateSuccess && (
                <div className="p-3 bg-emerald-950/60 border border-emerald-800/80 rounded-lg flex items-center justify-between">
                  <div className="flex items-center gap-2 text-emerald-400">
                    <CheckCircle2 className="w-4 h-4" />
                    <span className="font-bold">XML applied and verified!</span>
                  </div>
                  <button
                    onClick={onQuickReboot}
                    className="px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-bold transition-colors cursor-pointer"
                  >
                    Reboot Now
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right Column: XML Preview (5 cols) */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 flex flex-col h-full">
            <div className="flex items-center justify-between pb-3 border-b border-stone-800 mb-3">
              <div className="flex items-center gap-2 text-stone-200">
                <FileCode className="w-4 h-4 text-red-400" />
                <h3 className="text-sm font-bold">WifiConfigStoreSoftAp.xml</h3>
              </div>
              <button
                onClick={handleCopyXml}
                className="flex items-center gap-1 text-xs text-stone-400 hover:text-stone-200 font-mono px-2 py-1 bg-stone-800 rounded border border-stone-700 transition-colors"
              >
                {copiedXml ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                    <span className="text-emerald-400">Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    <span>Copy</span>
                  </>
                )}
              </button>
            </div>

            <div className="relative flex-1">
              <pre className="bg-black/70 p-3.5 rounded-lg border border-stone-900 text-emerald-400/90 font-mono text-[11px] leading-relaxed overflow-x-auto select-all max-h-[460px] overflow-y-auto">
                {config.rawXml}
              </pre>
            </div>

            <div className="mt-4 pt-3 border-t border-stone-800 text-[11px] font-mono text-stone-400 flex justify-between">
              <span>Permissions: chmod 600</span>
              <span>Owner: wifi:wifi</span>
            </div>
          </div>
        </div>
      </div>

      {/* Change Logs Audit Repository */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-stone-800 pb-3">
          <div className="flex items-center gap-2">
            <History className="w-5 h-5 text-red-400" />
            <h3 className="text-base font-bold text-white">Wi-Fi Change Log Audit Trail</h3>
          </div>
          <span className="text-xs text-stone-500 font-mono">
            {changeLogs.length} Events Recorded
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-stone-800 text-stone-400 bg-stone-950/40">
                <th className="py-2.5 px-3">Timestamp</th>
                <th className="py-2.5 px-3">Previous SSID</th>
                <th className="py-2.5 px-3">Updated SSID</th>
                <th className="py-2.5 px-3">Passphrase</th>
                <th className="py-2.5 px-3">Status</th>
                <th className="py-2.5 px-3">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-800/60">
              {changeLogs.map((log) => (
                <tr key={log.id} className="hover:bg-stone-800/40 transition-colors">
                  <td className="py-2.5 px-3 text-stone-400">{log.timestamp}</td>
                  <td className="py-2.5 px-3 text-stone-300 font-semibold">{log.oldSsid}</td>
                  <td className="py-2.5 px-3 text-emerald-400 font-bold">{log.newSsid}</td>
                  <td className="py-2.5 px-3 text-stone-400">{log.newPasswordMasked}</td>
                  <td className="py-2.5 px-3">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold ${
                        log.status === 'SUCCESS'
                          ? 'bg-emerald-950 text-emerald-400 border border-emerald-800/60'
                          : 'bg-red-950 text-red-400 border border-red-800/60'
                      }`}
                    >
                      {log.status}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-stone-400 truncate max-w-xs">{log.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

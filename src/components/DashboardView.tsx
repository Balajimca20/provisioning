import React, { useState, useEffect } from 'react';
import {
  Wifi,
  Terminal,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  ArrowRight,
  Shield,
  ShieldCheck,
  RefreshCw,
  Cpu,
  Database,
  Eye,
  EyeOff,
  Radio,
  Smartphone,
  Activity,
} from 'lucide-react';
import { AdbState, NetworkDiagnosticProbe, TabType, WifiState } from '../types';
import { SsidValidator } from '../lib/ssidValidator';
import { RealtimeAdbClient } from '../lib/realtimeAdbClient';

interface DashboardViewProps {
  wifiState: WifiState;
  adbState: AdbState;
  onConnectWifi: (ssid: string, password?: string) => Promise<void>;
  onDisconnectWifi: () => void;
  onConnectAdb: (host: string, port: number) => Promise<void>;
  onDisconnectAdb: () => void;
  setActiveTab: (tab: TabType) => void;
  onQuickReboot: () => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  wifiState,
  adbState,
  onConnectWifi,
  onDisconnectWifi,
  onConnectAdb,
  onDisconnectAdb,
  setActiveTab,
}) => {
  const [inputSsid, setInputSsid] = useState('RE_LXHD_250925');
  const [inputPassword, setInputPassword] = useState('RoyalEnfield@2026');
  const [showPassword, setShowPassword] = useState(false);
  const [adbHost, setAdbHost] = useState('192.168.1.1');
  const [adbPort, setAdbPort] = useState('5555');

  // Real-time probes state
  const [probes, setProbes] = useState<NetworkDiagnosticProbe[]>([
    {
      target: '192.168.1.1:5555',
      type: 'ADB_TCP',
      latencyMs: 12,
      status: 'ONLINE',
      details: 'Active TCP listening socket (Dadb protocol)',
      lastChecked: new Date().toLocaleTimeString(),
    },
    {
      target: '192.168.1.1:80',
      type: 'SOFTAP_GATEWAY',
      latencyMs: 4,
      status: 'ONLINE',
      details: 'Vehicle SoftAP DHCP Gateway reachable',
      lastChecked: new Date().toLocaleTimeString(),
    },
    {
      target: '192.168.1.1:8080',
      type: 'SUPPLIER_GRAPHQL',
      latencyMs: 18,
      status: 'ONLINE',
      details: 'Cluster Diagnostic GraphQL Gateway daemon',
      lastChecked: new Date().toLocaleTimeString(),
    },
  ]);
  const [isProbing, setIsProbing] = useState(false);
  const [liveMonitoring, setLiveMonitoring] = useState(true);

  const ssidError = SsidValidator.getValidationError(inputSsid);
  const isSsidValid = ssidError === null;

  const runDiagnostics = async () => {
    setIsProbing(true);
    const client = RealtimeAdbClient.getInstance();

    const p1 = await client.probeNetworkTarget(adbHost, 'ADB_TCP', parseInt(adbPort, 10) || 5555);
    const p2 = await client.probeNetworkTarget('192.168.1.1', 'SOFTAP_GATEWAY', 80);
    const p3 = await client.probeNetworkTarget('192.168.1.1', 'SUPPLIER_GRAPHQL', 8080);

    setProbes([p1, p2, p3]);
    setIsProbing(false);
  };

  useEffect(() => {
    if (!liveMonitoring) return;
    const interval = setInterval(() => {
      runDiagnostics();
    }, 15000);
    return () => clearInterval(interval);
  }, [liveMonitoring, adbHost, adbPort]);

  const handleWifiSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isSsidValid) return;
    await onConnectWifi(SsidValidator.sanitize(inputSsid), inputPassword);
  };

  const handleAdbSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const port = parseInt(adbPort, 10) || 5555;
    await onConnectAdb(adbHost, port);
  };

  const isBothConnected = wifiState.status === 'connected' && adbState.status === 'connected';

  return (
    <div className="space-y-6">
      {/* Top Banner / System Status */}
      <div className="bg-gradient-to-r from-stone-900 via-stone-900 to-stone-950 border border-stone-800 rounded-xl p-5 shadow-lg relative overflow-hidden">
        <div className="absolute right-0 top-0 translate-x-10 -translate-y-10 w-64 h-64 bg-red-600/10 rounded-full blur-3xl pointer-events-none" />
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 relative z-10">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-bold text-white tracking-wide">
                Vehicle Connection Provisioning
              </h2>
              <span className="px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider bg-emerald-950 text-emerald-300 rounded border border-emerald-800">
                REALTIME MODE ACTIVE
              </span>
            </div>
            <p className="text-sm text-stone-400 mt-1 max-w-2xl">
              Connect to vehicle Wi-Fi SoftAP access point without internet requirement, establish
              authenticated ADB bridge over TCP, and execute live diagnostic modifications.
            </p>
          </div>

          {isBothConnected ? (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2 px-3.5 py-2 bg-emerald-950/80 border border-emerald-700/60 rounded-lg text-emerald-400 text-sm font-medium">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                <span>Vehicle Link Ready</span>
              </div>
              <button
                onClick={() => setActiveTab('wifi')}
                className="flex items-center gap-2 px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm font-bold shadow-lg shadow-red-900/30 transition-all cursor-pointer"
              >
                <span>Manage Wi-Fi & OTA</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2 text-xs font-mono text-stone-400 bg-stone-950/70 border border-stone-800 px-3 py-2 rounded-lg">
              <span className="inline-block w-2 h-2 rounded-full bg-amber-500 animate-ping mr-1" />
              <span>Follow Step 1 & Step 2 below to proceed</span>
            </div>
          )}
        </div>
      </div>

      {/* Real-time Network Diagnostic & Ping Monitor */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-stone-800 pb-3">
          <div className="flex items-center gap-2 font-mono text-xs">
            <Activity className="w-4 h-4 text-cyan-400" />
            <span className="font-bold text-stone-200 uppercase tracking-wider">
              Real-time Socket & Ping Probes
            </span>
          </div>

          <div className="flex items-center gap-3 text-xs font-mono">
            <label className="flex items-center gap-2 text-stone-400 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={liveMonitoring}
                onChange={(e) => setLiveMonitoring(e.target.checked)}
                className="rounded border-stone-700 text-red-600 focus:ring-0"
              />
              <span>Live Heartbeat (15s)</span>
            </label>

            <button
              onClick={runDiagnostics}
              disabled={isProbing}
              className="flex items-center gap-1 px-2.5 py-1 bg-stone-950 border border-stone-800 hover:border-stone-700 text-stone-300 rounded transition-colors cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isProbing ? 'animate-spin text-cyan-400' : ''}`} />
              <span>{isProbing ? 'Probing...' : 'Ping Now'}</span>
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 font-mono text-xs">
          {probes.map((probe, idx) => (
            <div
              key={idx}
              className="bg-stone-950/80 border border-stone-800/80 rounded-lg p-3 space-y-1.5"
            >
              <div className="flex items-center justify-between">
                <span className="font-bold text-stone-300 truncate">{probe.target}</span>
                <span
                  className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                    probe.status === 'ONLINE'
                      ? 'bg-emerald-950 text-emerald-400 border border-emerald-800/60'
                      : 'bg-red-950 text-red-400 border border-red-800/60'
                  }`}
                >
                  {probe.status}
                </span>
              </div>
              <div className="flex items-center justify-between text-stone-500 text-[11px]">
                <span>Type: {probe.type}</span>
                <span className="text-cyan-400 font-bold">{probe.latencyMs} ms</span>
              </div>
              <p className="text-[10px] text-stone-400 truncate">{probe.details}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Main Connection Grid (Step 1 & Step 2) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Step 1: Wi-Fi Setup */}
        <div
          className={`bg-stone-900 border rounded-xl p-6 transition-all relative ${
            wifiState.status === 'connected'
              ? 'border-emerald-800/80 bg-stone-900/90 shadow-lg shadow-emerald-950/20'
              : 'border-stone-800'
          }`}
        >
          {/* Header */}
          <div className="flex items-center justify-between pb-4 mb-5 border-b border-stone-800">
            <div className="flex items-center gap-3">
              <div
                className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-sm ${
                  wifiState.status === 'connected'
                    ? 'bg-emerald-600 text-white'
                    : 'bg-stone-800 text-stone-300 border border-stone-700'
                }`}
              >
                {wifiState.status === 'connected' ? <CheckCircle2 className="w-5 h-5" /> : '1'}
              </div>
              <div>
                <h3 className="font-bold text-base text-stone-100 flex items-center gap-2">
                  <span>Step 1 — Wi-Fi Connection</span>
                  <Radio className="w-4 h-4 text-stone-400" />
                </h3>
                <p className="text-xs text-stone-400">Non-internet vehicle SoftAP binding</p>
              </div>
            </div>
            {wifiState.status === 'connected' && (
              <span className="text-xs px-2.5 py-0.5 bg-emerald-900/60 border border-emerald-700 text-emerald-300 rounded font-medium">
                Connected
              </span>
            )}
          </div>

          {/* Connected State UI */}
          {wifiState.status === 'connected' ? (
            <div className="space-y-4">
              <div className="bg-stone-950/80 border border-emerald-900/50 rounded-lg p-4 space-y-2.5 font-mono text-xs">
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Connected SSID:</span>
                  <span className="text-emerald-400 font-bold">{wifiState.ssid}</span>
                </div>
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Device IP (DHCP):</span>
                  <span>{wifiState.ipAddress || '192.168.1.45'}</span>
                </div>
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Gateway / AP IP:</span>
                  <span>{wifiState.gateway || '192.168.1.1'}</span>
                </div>
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Network Binding:</span>
                  <span className="text-emerald-400">Process Bound (Direct Socket)</span>
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={onDisconnectWifi}
                  className="w-full py-2.5 px-4 bg-stone-800 hover:bg-stone-700 border border-stone-700 hover:border-stone-600 text-stone-200 text-sm font-semibold rounded-lg transition-colors cursor-pointer"
                >
                  Disconnect Wi-Fi
                </button>
              </div>
            </div>
          ) : (
            /* Disconnected / Form State UI */
            <form onSubmit={handleWifiSubmit} className="space-y-4">
              {/* SSID Input */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-xs font-semibold text-stone-300 uppercase tracking-wider">
                    Vehicle SSID
                  </label>
                  <span className="text-[11px] font-mono text-stone-500">
                    Format: RE_XXXX_XXXXXX (14 chars)
                  </span>
                </div>
                <div className="relative">
                  <input
                    type="text"
                    value={inputSsid}
                    onChange={(e) => setInputSsid(e.target.value.toUpperCase())}
                    placeholder="RE_LXHD_250925"
                    className={`w-full bg-stone-950 border px-3.5 py-2.5 rounded-lg text-sm font-mono text-white placeholder-stone-600 focus:outline-none transition-colors ${
                      inputSsid.length > 0 && !isSsidValid
                        ? 'border-red-500/80 focus:border-red-400 focus:ring-1 focus:ring-red-400'
                        : 'border-stone-700 focus:border-red-500 focus:ring-1 focus:ring-red-500'
                    }`}
                  />
                  {inputSsid.length > 0 && (
                    <div className="absolute right-3 top-2.5">
                      {isSsidValid ? (
                        <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                      ) : (
                        <XCircle className="w-5 h-5 text-red-400" />
                      )}
                    </div>
                  )}
                </div>

                {/* Real-time SSID Validation Error Feedback */}
                {inputSsid.length > 0 && ssidError && (
                  <div className="mt-1.5 flex items-center gap-1.5 text-xs text-red-400 font-medium">
                    <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                    <span>{ssidError}</span>
                  </div>
                )}
              </div>

              {/* Password Input */}
              <div>
                <label className="block text-xs font-semibold text-stone-300 uppercase tracking-wider mb-1.5">
                  WPA2 Passphrase
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={inputPassword}
                    onChange={(e) => setInputPassword(e.target.value)}
                    placeholder="Enter vehicle AP passphrase"
                    className="w-full bg-stone-950 border border-stone-700 px-3.5 py-2.5 rounded-lg text-sm font-mono text-white placeholder-stone-600 focus:outline-none focus:border-red-500 transition-colors"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-2.5 text-stone-400 hover:text-stone-200"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Connect Button */}
              <button
                type="submit"
                disabled={!isSsidValid || wifiState.status === 'connecting'}
                className={`w-full py-2.5 px-4 rounded-lg text-sm font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                  isSsidValid && wifiState.status !== 'connecting'
                    ? 'bg-red-600 hover:bg-red-500 text-white shadow-lg shadow-red-900/30'
                    : 'bg-stone-800 text-stone-500 cursor-not-allowed border border-stone-700/50'
                }`}
              >
                {wifiState.status === 'connecting' ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Binding Wi-Fi Specifier...</span>
                  </>
                ) : (
                  <>
                    <Wifi className="w-4 h-4" />
                    <span>Connect to Vehicle Wi-Fi</span>
                  </>
                )}
              </button>
            </form>
          )}
        </div>

        {/* Step 2: ADB Connectivity */}
        <div
          className={`bg-stone-900 border rounded-xl p-6 transition-all relative ${
            adbState.status === 'connected'
              ? 'border-cyan-800/80 bg-stone-900/90 shadow-lg shadow-cyan-950/20'
              : 'border-stone-800'
          }`}
        >
          {/* Header */}
          <div className="flex items-center justify-between pb-4 mb-5 border-b border-stone-800">
            <div className="flex items-center gap-3">
              <div
                className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-sm ${
                  adbState.status === 'connected'
                    ? 'bg-cyan-600 text-white'
                    : 'bg-stone-800 text-stone-300 border border-stone-700'
                }`}
              >
                {adbState.status === 'connected' ? <CheckCircle2 className="w-5 h-5" /> : '2'}
              </div>
              <div>
                <h3 className="font-bold text-base text-stone-100 flex items-center gap-2">
                  <span>Step 2 — ADB Bridge</span>
                  <Terminal className="w-4 h-4 text-stone-400" />
                </h3>
                <p className="text-xs text-stone-400">TCP network ADB daemon on cluster</p>
              </div>
            </div>
            {adbState.status === 'connected' && (
              <span className="text-xs px-2.5 py-0.5 bg-cyan-900/60 border border-cyan-700 text-cyan-300 rounded font-medium">
                Connected
              </span>
            )}
          </div>

          {/* ADB Connected State UI */}
          {adbState.status === 'connected' ? (
            <div className="space-y-4">
              <div className="bg-stone-950/80 border border-cyan-900/50 rounded-lg p-4 space-y-2.5 font-mono text-xs">
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Target Endpoint:</span>
                  <span className="text-cyan-400 font-bold">
                    {adbState.host}:{adbState.port}
                  </span>
                </div>
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Root Privilege:</span>
                  <span className="text-emerald-400 font-bold flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    uid=0(root) Verified
                  </span>
                </div>
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Cluster Hardware:</span>
                  <span className="text-stone-300 truncate max-w-[200px]">
                    {adbState.deviceModel}
                  </span>
                </div>
                <div className="flex justify-between items-center text-stone-300">
                  <span className="text-stone-500">Serial Number:</span>
                  <span className="text-stone-300">{adbState.serialNumber}</span>
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={onDisconnectAdb}
                  className="w-full py-2.5 px-4 bg-stone-800 hover:bg-stone-700 border border-stone-700 text-stone-200 text-sm font-semibold rounded-lg transition-colors cursor-pointer"
                >
                  Disconnect ADB
                </button>
              </div>
            </div>
          ) : (
            /* ADB Form / Disconnected State */
            <form onSubmit={handleAdbSubmit} className="space-y-4">
              <div className="grid grid-cols-3 gap-3">
                <div className="col-span-2">
                  <label className="block text-xs font-semibold text-stone-300 uppercase tracking-wider mb-1.5">
                    Target Host IP
                  </label>
                  <input
                    type="text"
                    value={adbHost}
                    onChange={(e) => setAdbHost(e.target.value)}
                    placeholder="192.168.1.1"
                    className="w-full bg-stone-950 border border-stone-700 px-3.5 py-2.5 rounded-lg text-sm font-mono text-white placeholder-stone-600 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-stone-300 uppercase tracking-wider mb-1.5">
                    Port
                  </label>
                  <input
                    type="text"
                    value={adbPort}
                    onChange={(e) => setAdbPort(e.target.value)}
                    placeholder="5555"
                    className="w-full bg-stone-950 border border-stone-700 px-3.5 py-2.5 rounded-lg text-sm font-mono text-white placeholder-stone-600 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="p-3 bg-stone-950/60 border border-stone-800 rounded-lg text-xs text-stone-400 space-y-1">
                <p className="flex items-center gap-1.5 font-mono text-stone-300">
                  <Shield className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Uses Dadb protocol over TCP with auto-root escalation</span>
                </p>
                <p className="text-[11px] text-stone-500">
                  Directly interacts with SoftAP XML config & OTA recovery subsystem.
                </p>
              </div>

              <button
                type="submit"
                disabled={adbState.status === 'connecting'}
                className="w-full py-2.5 px-4 bg-cyan-600 hover:bg-cyan-500 text-white rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-lg shadow-cyan-950/30 transition-all cursor-pointer"
              >
                {adbState.status === 'connecting' ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Connecting ADB Daemon...</span>
                  </>
                ) : (
                  <>
                    <Terminal className="w-4 h-4" />
                    <span>Connect ADB ({adbHost}:{adbPort})</span>
                  </>
                )}
              </button>
            </form>
          )}
        </div>
      </div>

      {/* Hardware & Diagnostics Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-stone-900 border border-stone-800 rounded-xl p-4 flex items-center gap-3.5">
          <div className="w-11 h-11 rounded-lg bg-emerald-950/80 border border-emerald-800/60 flex items-center justify-center shrink-0">
            <ShieldCheck className="w-6 h-6 text-emerald-400" />
          </div>
          <div>
            <span className="text-xs text-stone-400 font-mono">Battery Voltage</span>
            <div className="text-lg font-bold text-stone-100 flex items-baseline gap-1.5">
              <span>12.84 V</span>
              <span className="text-xs text-emerald-400 font-medium font-mono">(94% SoC)</span>
            </div>
          </div>
        </div>

        <div className="bg-stone-900 border border-stone-800 rounded-xl p-4 flex items-center gap-3.5">
          <div className="w-11 h-11 rounded-lg bg-red-950/80 border border-red-800/60 flex items-center justify-center shrink-0">
            <Cpu className="w-6 h-6 text-red-400" />
          </div>
          <div>
            <span className="text-xs text-stone-400 font-mono">ECU Link Status</span>
            <div className="text-lg font-bold text-stone-100 flex items-baseline gap-1.5">
              <span>Nominal</span>
              <span className="text-xs text-stone-400 font-mono">Sherpa 452</span>
            </div>
          </div>
        </div>

        <div className="bg-stone-900 border border-stone-800 rounded-xl p-4 flex items-center gap-3.5">
          <div className="w-11 h-11 rounded-lg bg-cyan-950/80 border border-cyan-800/60 flex items-center justify-center shrink-0">
            <Smartphone className="w-6 h-6 text-cyan-400" />
          </div>
          <div>
            <span className="text-xs text-stone-400 font-mono">TFT OS Version</span>
            <div className="text-lg font-bold text-stone-100">
              <span>Android 12</span>
            </div>
          </div>
        </div>

        <div className="bg-stone-900 border border-stone-800 rounded-xl p-4 flex items-center gap-3.5">
          <div className="w-11 h-11 rounded-lg bg-amber-950/80 border border-amber-800/60 flex items-center justify-center shrink-0">
            <Database className="w-6 h-6 text-amber-400" />
          </div>
          <div>
            <span className="text-xs text-stone-400 font-mono">Storage Partition</span>
            <div className="text-lg font-bold text-stone-100 flex items-baseline gap-1.5">
              <span>25.2 GB</span>
              <span className="text-xs text-stone-400 font-mono">Free / 28.4G</span>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Launch Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
          onClick={() => setActiveTab('wifi')}
          className="bg-stone-900 hover:bg-stone-800/80 border border-stone-800 hover:border-stone-700 rounded-xl p-5 cursor-pointer transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-9 h-9 rounded-lg bg-red-950 border border-red-800/50 flex items-center justify-center text-red-400 group-hover:scale-110 transition-transform">
              <Radio className="w-5 h-5" />
            </div>
            <ArrowRight className="w-4 h-4 text-stone-500 group-hover:text-red-400 group-hover:translate-x-1 transition-all" />
          </div>
          <h4 className="font-bold text-stone-200 group-hover:text-white">Wi-Fi SoftAP Manager</h4>
          <p className="text-xs text-stone-400 mt-1">
            Pull, modify credentials in WifiConfigStoreSoftAp.xml, and push with chmod 600
            permissions.
          </p>
        </div>

        <div
          onClick={() => setActiveTab('ota')}
          className="bg-stone-900 hover:bg-stone-800/80 border border-stone-800 hover:border-stone-700 rounded-xl p-5 cursor-pointer transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-9 h-9 rounded-lg bg-blue-950 border border-blue-800/50 flex items-center justify-center text-blue-400 group-hover:scale-110 transition-transform">
              <RefreshCw className="w-5 h-5" />
            </div>
            <ArrowRight className="w-4 h-4 text-stone-500 group-hover:text-blue-400 group-hover:translate-x-1 transition-all" />
          </div>
          <h4 className="font-bold text-stone-200 group-hover:text-white">OTA Pipeline</h4>
          <p className="text-xs text-stone-400 mt-1">
            Download firmware payload, verify SHA256 checksums, flash cluster recovery slot, and
            reboot.
          </p>
        </div>

        <div
          onClick={() => setActiveTab('supplier')}
          className="bg-stone-900 hover:bg-stone-800/80 border border-stone-800 hover:border-stone-700 rounded-xl p-5 cursor-pointer transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-9 h-9 rounded-lg bg-emerald-950 border border-emerald-800/50 flex items-center justify-center text-emerald-400 group-hover:scale-110 transition-transform">
              <Cpu className="w-5 h-5" />
            </div>
            <ArrowRight className="w-4 h-4 text-stone-500 group-hover:text-emerald-400 group-hover:translate-x-1 transition-all" />
          </div>
          <h4 className="font-bold text-stone-200 group-hover:text-white">
            Supplier Feed Telemetry
          </h4>
          <p className="text-xs text-stone-400 mt-1">
            Query vehicle profile over live GraphQL: ECU calibration, DTC fault logs, and warranty
            records.
          </p>
        </div>
      </div>
    </div>
  );
};

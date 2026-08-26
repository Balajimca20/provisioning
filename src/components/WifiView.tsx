import React, { useState } from 'react';
import { 
  Wifi, 
  Lock, 
  RotateCw, 
  CheckCircle, 
  AlertCircle, 
  Signal, 
  Shield, 
  RefreshCw,
  Server
} from 'lucide-react';
import type { DeviceConnectionState } from '../types';

interface WifiViewProps {
  deviceState: DeviceConnectionState;
  onConnectWifi: (ssid: string) => void;
  onConnectAdb: (host: string, port: string) => void;
  onDisconnect: () => void;
}

export const WifiView: React.FC<WifiViewProps> = ({
  deviceState,
  onConnectWifi,
  onConnectAdb,
  onDisconnect
}) => {
  const [ssid, setSsid] = useState('RE_Hunter_350_AP');
  const [password, setPassword] = useState('EnfieldSecure2024');
  const [adbHost, setAdbHost] = useState(deviceState.adbHost || '192.168.43.1');
  const [adbPort, setAdbPort] = useState(deviceState.adbPort || '5555');
  const [isConnectingWifi, setIsConnectingWifi] = useState(false);
  const [isConnectingAdb, setIsConnectingAdb] = useState(false);

  const handleWifiSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsConnectingWifi(true);
    await new Promise(r => setTimeout(r, 600));
    setIsConnectingWifi(false);
    onConnectWifi(ssid);
  };

  const handleAdbSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsConnectingAdb(true);
    await new Promise(r => setTimeout(r, 600));
    setIsConnectingAdb(false);
    onConnectAdb(adbHost, adbPort);
  };

  return (
    <div id="wifi-view-container" className="space-y-6 max-w-4xl mx-auto">
      <div className="bg-[#161920] border border-white/10 rounded-2xl p-5">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-blue-500" />
          <h2 className="text-base font-bold text-white tracking-wide">Vehicle Wi-Fi SoftAP & ADB Bridge</h2>
        </div>
        <p className="text-xs text-slate-400 mt-1">
          Pair with motorcycle telematics control unit (TCU) via Wi-Fi hotspot, then establish ADB service tunnel.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Step 1: Wi-Fi Hotspot Link */}
        <div className="bg-[#161920] border border-white/10 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-blue-400 font-mono">Step 1 • SoftAP Wi-Fi</span>
            {deviceState.isWifiConnected ? (
              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20">
                Connected
              </span>
            ) : (
              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-white/5 text-slate-400">
                Disconnected
              </span>
            )}
          </div>

          <form onSubmit={handleWifiSubmit} className="space-y-3">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Vehicle SSID</label>
              <div className="relative">
                <input
                  id="wifi-ssid-input"
                  type="text"
                  value={ssid}
                  onChange={(e) => setSsid(e.target.value)}
                  placeholder="e.g. RE_Himalayan_AP"
                  className="w-full bg-black/60 border border-white/15 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-blue-500"
                  required
                />
                <Wifi className="w-4 h-4 text-slate-500 absolute right-3 top-2.5" />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Passphrase</label>
              <div className="relative">
                <input
                  id="wifi-password-input"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="WPA2/WPA3 key"
                  className="w-full bg-black/60 border border-white/15 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-blue-500"
                  required
                />
                <Lock className="w-4 h-4 text-slate-500 absolute right-3 top-2.5" />
              </div>
            </div>

            <button
              id="connect-wifi-btn"
              type="submit"
              disabled={isConnectingWifi}
              className="w-full py-2.5 px-4 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs flex items-center justify-center gap-2 transition-colors mt-2"
            >
              {isConnectingWifi ? (
                <>
                  <RotateCw className="w-3.5 h-3.5 animate-spin" />
                  Connecting SoftAP...
                </>
              ) : deviceState.isWifiConnected ? (
                <>
                  <CheckCircle className="w-3.5 h-3.5" />
                  Re-sync Wi-Fi Link
                </>
              ) : (
                'Connect to Vehicle SoftAP'
              )}
            </button>
          </form>
        </div>

        {/* Step 2: ADB Network Bridge */}
        <div className={`bg-[#161920] border rounded-xl p-5 space-y-4 transition-all ${
          deviceState.isWifiConnected ? 'border-white/10' : 'border-white/5 opacity-60'
        }`}>
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-400 font-mono">Step 2 • ADB Tunnel</span>
            {deviceState.isAdbConnected ? (
              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20">
                Linked (Port 5555)
              </span>
            ) : (
              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-white/5 text-slate-400">
                Inactive
              </span>
            )}
          </div>

          <form onSubmit={handleAdbSubmit} className="space-y-3">
            <div className="grid grid-cols-3 gap-2">
              <div className="col-span-2">
                <label className="block text-xs font-medium text-slate-300 mb-1">Vehicle IP</label>
                <input
                  id="adb-ip-input"
                  type="text"
                  value={adbHost}
                  onChange={(e) => setAdbHost(e.target.value)}
                  placeholder="192.168.43.1"
                  disabled={!deviceState.isWifiConnected}
                  className="w-full bg-black/60 border border-white/15 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-emerald-500 disabled:opacity-50"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Port</label>
                <input
                  id="adb-port-input"
                  type="text"
                  value={adbPort}
                  onChange={(e) => setAdbPort(e.target.value)}
                  placeholder="5555"
                  disabled={!deviceState.isWifiConnected}
                  className="w-full bg-black/60 border border-white/15 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-emerald-500 disabled:opacity-50"
                  required
                />
              </div>
            </div>

            <button
              id="connect-adb-btn"
              type="submit"
              disabled={!deviceState.isWifiConnected || isConnectingAdb}
              className="w-full py-2.5 px-4 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-white font-bold text-xs flex items-center justify-center gap-2 transition-colors mt-2"
            >
              {isConnectingAdb ? (
                <>
                  <RotateCw className="w-3.5 h-3.5 animate-spin" />
                  Bridging ADB...
                </>
              ) : deviceState.isAdbConnected ? (
                <>
                  <CheckCircle className="w-3.5 h-3.5" />
                  Bridge Active
                </>
              ) : (
                'Establish ADB Bridge'
              )}
            </button>
          </form>

          {deviceState.isAdbConnected && (
            <button
              onClick={onDisconnect}
              className="w-full py-2 px-3 rounded-lg border border-rose-800/40 text-rose-400 hover:bg-rose-950/30 text-xs font-semibold transition-colors"
            >
              Terminate All Links
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

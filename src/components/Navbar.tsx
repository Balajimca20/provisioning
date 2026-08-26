import React from 'react';
import { 
  Wifi, 
  Terminal, 
  LayoutDashboard, 
  Cpu, 
  Radio, 
  Zap, 
  ShieldCheck, 
  RefreshCw,
  Sliders
} from 'lucide-react';
import type { TabType, DeviceConnectionState } from '../types';

interface NavbarProps {
  activeTab: TabType;
  setActiveTab: (tab: TabType) => void;
  deviceState: DeviceConnectionState;
  onDisconnect: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  deviceState,
  onDisconnect
}) => {
  return (
    <header id="app-navbar" className="bg-[#161920] border-b border-white/10 px-4 py-3 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Brand & App Title */}
        <div className="flex items-center gap-3 w-full md:w-auto justify-between md:justify-start">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-lg bg-[#E53935]/15 border border-[#E53935]/30 flex items-center justify-center text-[#E53935]">
              <Zap className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-sm tracking-wider text-[#E53935]">FF PROVISIONING</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/10 font-mono text-slate-300 font-semibold">
                  {deviceState.buildVariant}
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Vehicle Connectivity & Firmware Pipeline</p>
            </div>
          </div>

          {/* Mobile Connection Pill */}
          <div className="flex md:hidden items-center gap-2">
            <div className={`w-2 h-2 rounded-full ${deviceState.isAdbConnected ? 'bg-emerald-400 animate-pulse' : deviceState.isWifiConnected ? 'bg-amber-400' : 'bg-rose-500'}`} />
            <span className="text-xs font-mono text-slate-300">
              {deviceState.isAdbConnected ? 'ADB LINKED' : deviceState.isWifiConnected ? 'WIFI ONLY' : 'OFFLINE'}
            </span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="flex items-center gap-1 overflow-x-auto w-full md:w-auto pb-1 md:pb-0 scrollbar-none">
          <button
            id="nav-tab-dashboard"
            onClick={() => setActiveTab('dashboard')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === 'dashboard'
                ? 'bg-[#E53935] text-white shadow-sm'
                : 'text-slate-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <LayoutDashboard className="w-3.5 h-3.5" />
            Dashboard
          </button>

          <button
            id="nav-tab-cmd-ota"
            onClick={() => setActiveTab('cmd_ota')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === 'cmd_ota'
                ? 'bg-[#00D2B4] text-slate-950 font-bold shadow-sm'
                : 'text-slate-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <Zap className="w-3.5 h-3.5" />
            CommandLine OTA
          </button>

          <button
            id="nav-tab-ota"
            onClick={() => setActiveTab('ota')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === 'ota'
                ? 'bg-amber-500 text-slate-950 font-bold shadow-sm'
                : 'text-slate-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Standard OTA
          </button>

          <button
            id="nav-tab-wifi"
            onClick={() => setActiveTab('wifi')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === 'wifi'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-slate-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <Wifi className="w-3.5 h-3.5" />
            SoftAP Wi-Fi
          </button>

          <button
            id="nav-tab-terminal"
            onClick={() => setActiveTab('terminal')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === 'terminal'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <Terminal className="w-3.5 h-3.5" />
            ADB Terminal
          </button>

          <button
            id="nav-tab-supplier"
            onClick={() => setActiveTab('supplier')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === 'supplier'
                ? 'bg-purple-600 text-white shadow-sm'
                : 'text-slate-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            Supplier Telemetry
          </button>
        </nav>

        {/* Status Indicators & Session Controls */}
        <div className="hidden md:flex items-center gap-3">
          <div className="flex items-center gap-2 bg-[#0F1115] border border-white/10 px-2.5 py-1.5 rounded-lg text-xs font-mono">
            <div className={`w-2 h-2 rounded-full ${deviceState.isAdbConnected ? 'bg-emerald-400 animate-pulse' : deviceState.isWifiConnected ? 'bg-amber-400' : 'bg-rose-500'}`} />
            <span className="text-slate-300 text-[11px]">
              {deviceState.isAdbConnected ? `${deviceState.adbHost}:${deviceState.adbPort}` : deviceState.isWifiConnected ? deviceState.connectedSsid : 'No Link'}
            </span>
          </div>

          {(deviceState.isWifiConnected || deviceState.isAdbConnected) && (
            <button
              id="disconnect-btn"
              onClick={onDisconnect}
              className="text-[11px] font-semibold text-rose-400 hover:text-rose-300 hover:bg-rose-950/40 border border-rose-800/40 px-2.5 py-1.5 rounded-lg transition-colors"
            >
              Disconnect
            </button>
          )}
        </div>
      </div>
    </header>
  );
};

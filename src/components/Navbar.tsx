import React from 'react';
import { Wifi, Terminal, Cpu, Radio, Activity, Wrench, RefreshCw, Power } from 'lucide-react';
import { AdbState, EnvironmentType, TabType, WifiState } from '../types';

interface NavbarProps {
  activeTab: TabType;
  setActiveTab: (tab: TabType) => void;
  wifiState: WifiState;
  adbState: AdbState;
  env: EnvironmentType;
  setEnv: (env: EnvironmentType) => void;
  onQuickReboot: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  wifiState,
  adbState,
  env,
  setEnv,
  onQuickReboot,
}) => {
  return (
    <header className="bg-stone-900/90 border-b border-stone-800 backdrop-blur sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-gradient-to-br from-red-600 to-red-800 flex items-center justify-center shadow-lg shadow-red-900/20 border border-red-500/30">
              <Wrench className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold tracking-wider text-white text-base sm:text-lg uppercase">
                  FF PROVISIONING
                </span>
              </div>
              <p className="text-xs text-stone-400 font-mono hidden sm:block">
                Vehicle Provisioning & Diagnostics Suite
              </p>
            </div>
          </div>

          {/* Status Chips */}
          <div className="hidden md:flex items-center gap-3 font-mono text-xs">
            {/* Wi-Fi Indicator */}
            <div
              className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md border transition-colors ${
                wifiState.status === 'connected'
                  ? 'bg-emerald-950/50 border-emerald-800/60 text-emerald-400'
                  : wifiState.status === 'connecting'
                  ? 'bg-amber-950/50 border-amber-800/60 text-amber-400 animate-pulse'
                  : 'bg-stone-800/50 border-stone-700 text-stone-400'
              }`}
            >
              <Wifi className="w-3.5 h-3.5" />
              <span>
                {wifiState.status === 'connected'
                  ? wifiState.ssid
                  : wifiState.status === 'connecting'
                  ? 'Connecting...'
                  : 'Wi-Fi Offline'}
              </span>
            </div>

            {/* ADB Indicator */}
            <div
              className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md border transition-colors ${
                adbState.status === 'connected'
                  ? 'bg-cyan-950/50 border-cyan-800/60 text-cyan-400'
                  : adbState.status === 'connecting'
                  ? 'bg-amber-950/50 border-amber-800/60 text-amber-400 animate-pulse'
                  : 'bg-stone-800/50 border-stone-700 text-stone-400'
              }`}
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>
                {adbState.status === 'connected'
                  ? `${adbState.host}:${adbState.port} ${adbState.isRoot ? '(root)' : ''}`
                  : adbState.status === 'connecting'
                  ? 'ADB Connecting...'
                  : 'ADB Offline'}
              </span>
            </div>

            {/* Environment Selector */}
            <div className="flex items-center bg-stone-950 border border-stone-800 rounded-md p-0.5">
              {(['dev', 'uat', 'prod'] as EnvironmentType[]).map((e) => (
                <button
                  key={e}
                  onClick={() => setEnv(e)}
                  className={`px-2 py-0.5 rounded text-[11px] font-semibold uppercase transition-all ${
                    env === e
                      ? 'bg-stone-800 text-stone-100 shadow-sm'
                      : 'text-stone-500 hover:text-stone-300'
                  }`}
                >
                  {e}
                </button>
              ))}
            </div>

            {/* Reboot shortcut if ADB connected */}
            {adbState.status === 'connected' && (
              <button
                onClick={onQuickReboot}
                title="Send ADB reboot command"
                className="flex items-center gap-1 px-2.5 py-1 rounded-md bg-stone-800 hover:bg-red-900/60 hover:border-red-700 border border-stone-700 text-stone-300 hover:text-red-200 transition-colors"
              >
                <Power className="w-3.5 h-3.5 text-red-400" />
                <span>Reboot</span>
              </button>
            )}
          </div>
        </div>

        {/* Tab Navigation Bar */}
        <nav className="flex space-x-1 sm:space-x-2 border-t border-stone-800/70 pt-1 pb-2 overflow-x-auto">
          <button
            onClick={() => setActiveTab('dashboard')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              activeTab === 'dashboard'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/30'
                : 'text-stone-400 hover:text-stone-200 hover:bg-stone-800/60'
            }`}
          >
            <Activity className="w-4 h-4" />
            <span>Dashboard</span>
          </button>

          <button
            onClick={() => setActiveTab('wifi')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              activeTab === 'wifi'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/30'
                : 'text-stone-400 hover:text-stone-200 hover:bg-stone-800/60'
            }`}
          >
            <Radio className="w-4 h-4" />
            <span>Wi-Fi Manager</span>
          </button>

          <button
            onClick={() => setActiveTab('ota')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              activeTab === 'ota'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/30'
                : 'text-stone-400 hover:text-stone-200 hover:bg-stone-800/60'
            }`}
          >
            <RefreshCw className="w-4 h-4" />
            <span>OTA Pipeline</span>
          </button>

          <button
            onClick={() => setActiveTab('supplier')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              activeTab === 'supplier'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/30'
                : 'text-stone-400 hover:text-stone-200 hover:bg-stone-800/60'
            }`}
          >
            <Cpu className="w-4 h-4" />
            <span>Supplier Feed</span>
          </button>

          <button
            onClick={() => setActiveTab('terminal')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              activeTab === 'terminal'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/30'
                : 'text-stone-400 hover:text-stone-200 hover:bg-stone-800/60'
            }`}
          >
            <Terminal className="w-4 h-4" />
            <span>ADB Terminal</span>
          </button>
        </nav>
      </div>
    </header>
  );
};

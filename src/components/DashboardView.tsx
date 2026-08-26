import React from 'react';
import { 
  BatteryCharging, 
  Gauge, 
  HardDrive, 
  Layers, 
  Wifi, 
  RefreshCw, 
  Radio, 
  Terminal, 
  Zap, 
  ArrowRight, 
  ShieldCheck, 
  Sliders, 
  Activity,
  Cpu
} from 'lucide-react';
import type { TabType, DeviceConnectionState } from '../types';

interface DashboardViewProps {
  deviceState: DeviceConnectionState;
  setActiveTab: (tab: TabType) => void;
  onUpdateVariant: (variant: 'DEV' | 'UAT' | 'PROD') => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  deviceState,
  setActiveTab,
  onUpdateVariant
}) => {
  return (
    <div id="dashboard-view-container" className="space-y-6">
      {/* Top Banner / Environment Switcher */}
      <div className="bg-[#161920] border border-white/10 rounded-2xl p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#E53935]" />
            <h1 className="text-lg font-black text-white tracking-wide">VEHICLE PROVISIONING SUITE</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Royal Enfield Field Service & Firmware Automation Environment
          </p>
        </div>

        <div className="flex items-center gap-2 bg-[#0F1115] border border-white/10 p-1 rounded-xl">
          <span className="text-[11px] font-mono text-slate-400 px-2">Flavor:</span>
          {(['DEV', 'UAT', 'PROD'] as const).map(variant => (
            <button
              key={variant}
              onClick={() => onUpdateVariant(variant)}
              className={`text-xs px-3 py-1 rounded-lg font-mono font-bold transition-all ${
                deviceState.buildVariant === variant
                  ? 'bg-[#E53935] text-white shadow-sm'
                  : 'text-slate-400 hover:text-white hover:bg-white/5'
              }`}
            >
              {variant}
            </button>
          ))}
        </div>
      </div>

      {/* Live Telemetry KPI Cards */}
      <div>
        <div className="flex items-center gap-2 mb-3 px-1">
          <Activity className="w-4 h-4 text-[#E53935]" />
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-300">Live Vehicle Telemetry</h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Battery Voltage */}
          <div className="bg-[#161920] border border-white/10 rounded-xl p-4 flex items-center justify-between">
            <div className="space-y-1">
              <span className="text-[11px] text-slate-400">Battery Level</span>
              <p className="text-xl font-black text-emerald-400 font-mono">{deviceState.batteryVoltage}</p>
              <span className="text-[10px] text-slate-500 font-mono">Nominal Range: 12.8V - 14.4V</span>
            </div>
            <div className="p-3 rounded-xl bg-emerald-500/10 text-emerald-400">
              <BatteryCharging className="w-6 h-6" />
            </div>
          </div>

          {/* ECU CAN Link */}
          <div className="bg-[#161920] border border-white/10 rounded-xl p-4 flex items-center justify-between">
            <div className="space-y-1">
              <span className="text-[11px] text-slate-400">ECU Bus Link</span>
              <p className="text-xl font-black text-blue-400 font-mono">{deviceState.ecuStatus}</p>
              <span className="text-[10px] text-slate-500 font-mono">Baud: 500 kbps (CAN 2.0B)</span>
            </div>
            <div className="p-3 rounded-xl bg-blue-500/10 text-blue-400">
              <Cpu className="w-6 h-6" />
            </div>
          </div>

          {/* eMMC Storage Space */}
          <div className="bg-[#161920] border border-white/10 rounded-xl p-4 flex items-center justify-between">
            <div className="space-y-1">
              <span className="text-[11px] text-slate-400">Storage Partition</span>
              <p className="text-xl font-black text-amber-400 font-mono">{deviceState.storageSpace}</p>
              <span className="text-[10px] text-slate-500 font-mono">userdata mount (/data)</span>
            </div>
            <div className="p-3 rounded-xl bg-amber-500/10 text-amber-400">
              <HardDrive className="w-6 h-6" />
            </div>
          </div>

          {/* Active Boot Slot */}
          <div className="bg-[#161920] border border-white/10 rounded-xl p-4 flex items-center justify-between">
            <div className="space-y-1">
              <span className="text-[11px] text-slate-400">Active Boot Slot</span>
              <p className="text-xl font-black text-[#00D2B4] font-mono">Slot {deviceState.activeSlot}</p>
              <span className="text-[10px] text-slate-500 font-mono">Target: Slot {deviceState.activeSlot === 'A' ? 'B' : 'A'}</span>
            </div>
            <div className="p-3 rounded-xl bg-[#00D2B4]/10 text-[#00D2B4]">
              <Layers className="w-6 h-6" />
            </div>
          </div>
        </div>
      </div>

      {/* Primary Action Modules Grid */}
      <div>
        <div className="flex items-center gap-2 mb-3 px-1">
          <Sliders className="w-4 h-4 text-slate-400" />
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-300">Service & Provisioning Modules</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {/* CommandLine OTA Card */}
          <div 
            id="module-card-cmd-ota"
            onClick={() => setActiveTab('cmd_ota')}
            className="bg-[#161920] hover:bg-[#1c202a] border border-white/10 hover:border-[#00D2B4]/50 rounded-xl p-5 cursor-pointer transition-all group relative overflow-hidden"
          >
            <div className="w-10 h-10 rounded-xl bg-[#00D2B4]/15 text-[#00D2B4] flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <Zap className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-[#00D2B4] transition-colors">
              Command Line OTA Upgrade
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Push local update.zip payload with root escalation, live progress streaming, and slot switching.
            </p>
            <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-[#00D2B4]">
              <span>Open Pipeline</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* Standard OTA Card */}
          <div 
            id="module-card-ota"
            onClick={() => setActiveTab('ota')}
            className="bg-[#161920] hover:bg-[#1c202a] border border-white/10 hover:border-amber-500/50 rounded-xl p-5 cursor-pointer transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-amber-500/15 text-amber-400 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <RefreshCw className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-amber-400 transition-colors">
              Standard OTA Cloud Pipeline
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Check Royal Enfield cloud repository for signed vehicle delta packages and firmware releases.
            </p>
            <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-amber-400">
              <span>Check Cloud Updates</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* SoftAP Wi-Fi Card */}
          <div 
            id="module-card-wifi"
            onClick={() => setActiveTab('wifi')}
            className="bg-[#161920] hover:bg-[#1c202a] border border-white/10 hover:border-blue-500/50 rounded-xl p-5 cursor-pointer transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-blue-500/15 text-blue-400 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <Wifi className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-blue-400 transition-colors">
              Vehicle SoftAP Link
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Configure Wi-Fi connection, sync SSID credentials, and verify local hotspot communication.
            </p>
            <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-blue-400">
              <span>Manage Network</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* ADB Shell Terminal */}
          <div 
            id="module-card-terminal"
            onClick={() => setActiveTab('terminal')}
            className="bg-[#161920] hover:bg-[#1c202a] border border-white/10 hover:border-emerald-500/50 rounded-xl p-5 cursor-pointer transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-emerald-500/15 text-emerald-400 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <Terminal className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-emerald-400 transition-colors">
              ADB Shell & Direct Terminal
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Execute low-level shell commands, view logcat streams, and inspect system daemon statuses.
            </p>
            <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-emerald-400">
              <span>Open Terminal</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* Supplier Telemetry Feed */}
          <div 
            id="module-card-supplier"
            onClick={() => setActiveTab('supplier')}
            className="bg-[#161920] hover:bg-[#1c202a] border border-white/10 hover:border-purple-500/50 rounded-xl p-5 cursor-pointer transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-purple-500/15 text-purple-400 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <Radio className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white group-hover:text-purple-400 transition-colors">
              Supplier Feed & Diagnostics
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Broadcast vehicle sensor snapshots, fuel calibration data, and hardware serial registers.
            </p>
            <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-purple-400">
              <span>View Telemetry</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* Android App Status & Gradle Build info */}
          <div className="bg-[#161920] border border-white/10 rounded-xl p-5 space-y-3">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
              <h3 className="text-sm font-bold text-white">Android Native Module</h3>
            </div>
            <p className="text-xs text-slate-400">
              Module <code className="text-slate-200 bg-white/5 px-1 py-0.5 rounded font-mono">:app</code> compiled with Jetpack Compose & Koin DI.
            </p>
            <div className="bg-black/50 p-2.5 rounded-lg border border-white/10 font-mono text-[11px] text-slate-300 space-y-1">
              <div>Build: <span className="text-emerald-400">assembleProdDebug SUCCESS</span></div>
              <div>Package: <span className="text-slate-400">com.royalenfield.provisioning</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

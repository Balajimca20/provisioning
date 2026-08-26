import React, { useState } from 'react';
import { Radio, RefreshCw, Activity, Cpu, Fuel, Thermometer, ShieldAlert, CheckCircle2 } from 'lucide-react';
import type { DeviceConnectionState } from '../types';

interface SupplierFeedViewProps {
  deviceState: DeviceConnectionState;
}

export const SupplierFeedView: React.FC<SupplierFeedViewProps> = ({ deviceState }) => {
  const [isStreaming, setIsStreaming] = useState(true);
  const [telemetry, setTelemetry] = useState({
    rpm: 1250,
    speedKmph: 0,
    coolantTempC: 84.5,
    fuelLevelPct: 78,
    throttlePosPct: 0.0,
    gear: 'N',
    supplierPartNo: 'RE-ECU-2024-BOSCH-7719',
    serialNum: 'RE-HNT350-IN-9812401',
    crcStatus: 'VALID (0x8F9A)'
  });

  return (
    <div id="supplier-feed-container" className="space-y-6">
      <div className="bg-[#161920] border border-white/10 rounded-2xl p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-purple-500" />
            <h2 className="text-base font-bold text-white tracking-wide">Supplier Telemetry & CAN Feed</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Real-time sensor parsing from Tier-1 powertrain ECU supplier (Bosch / Continental CAN matrices).
          </p>
        </div>

        <button
          onClick={() => setIsStreaming(!isStreaming)}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-colors ${
            isStreaming 
              ? 'bg-purple-600 hover:bg-purple-500 text-white' 
              : 'bg-white/10 text-slate-400'
          }`}
        >
          <Activity className={`w-3.5 h-3.5 ${isStreaming ? 'animate-pulse' : ''}`} />
          {isStreaming ? 'Live CAN Feed Active' : 'Feed Paused'}
        </button>
      </div>

      {/* Sensor Matrices */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-[#161920] border border-white/10 rounded-xl p-4 space-y-1">
          <span className="text-[11px] text-slate-400">Engine Idle / RPM</span>
          <p className="text-xl font-mono font-black text-purple-400">{telemetry.rpm} RPM</p>
          <span className="text-[10px] text-slate-500 font-mono">Idle Spec: 1200 ± 50</span>
        </div>

        <div className="bg-[#161920] border border-white/10 rounded-xl p-4 space-y-1">
          <span className="text-[11px] text-slate-400">Coolant Temp</span>
          <p className="text-xl font-mono font-black text-amber-400">{telemetry.coolantTempC} °C</p>
          <span className="text-[10px] text-slate-500 font-mono">Operating Band: 80-95°C</span>
        </div>

        <div className="bg-[#161920] border border-white/10 rounded-xl p-4 space-y-1">
          <span className="text-[11px] text-slate-400">Fuel Tank Level</span>
          <p className="text-xl font-mono font-black text-emerald-400">{telemetry.fuelLevelPct}%</p>
          <span className="text-[10px] text-slate-500 font-mono">Cap: 13.0 Liters</span>
        </div>

        <div className="bg-[#161920] border border-white/10 rounded-xl p-4 space-y-1">
          <span className="text-[11px] text-slate-400">Gear / Throttle</span>
          <p className="text-xl font-mono font-black text-cyan-400">Gear {telemetry.gear} ({telemetry.throttlePosPct}%)</p>
          <span className="text-[10px] text-slate-500 font-mono">TPS Sensor Calibrated</span>
        </div>
      </div>

      {/* Hardware Identifier Card */}
      <div className="bg-[#161920] border border-white/10 rounded-xl p-5 space-y-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">ECU Identity & Serial Registry</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 font-mono text-xs">
          <div className="bg-black/50 p-3 rounded-lg border border-white/10">
            <span className="text-slate-500 text-[10px]">SUPPLIER PART NUMBER</span>
            <p className="text-slate-200 font-bold mt-0.5">{telemetry.supplierPartNo}</p>
          </div>
          <div className="bg-black/50 p-3 rounded-lg border border-white/10">
            <span className="text-slate-500 text-[10px]">VEHICLE SERIAL NUMBER</span>
            <p className="text-slate-200 font-bold mt-0.5">{telemetry.serialNum}</p>
          </div>
          <div className="bg-black/50 p-3 rounded-lg border border-white/10">
            <span className="text-slate-500 text-[10px]">FRAME CRC INTEGRITY</span>
            <p className="text-emerald-400 font-bold mt-0.5">{telemetry.crcStatus}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

import React, { useState } from 'react';
import { 
  RefreshCw, 
  Download, 
  CheckCircle2, 
  AlertCircle, 
  Layers, 
  ShieldCheck, 
  Server, 
  Clock, 
  ArrowRight,
  RotateCw
} from 'lucide-react';
import type { DeviceConnectionState } from '../types';

interface OtaViewProps {
  deviceState: DeviceConnectionState;
}

export const OtaView: React.FC<OtaViewProps> = ({ deviceState }) => {
  const [isChecking, setIsChecking] = useState(false);
  const [availableVersion, setAvailableVersion] = useState<string | null>('v2.4.1-OTA-STABLE');
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [isInstalling, setIsInstalling] = useState(false);
  const [installProgress, setInstallProgress] = useState(0);
  const [isComplete, setIsComplete] = useState(false);

  const checkCloudUpdates = async () => {
    setIsChecking(true);
    await new Promise(r => setTimeout(r, 1000));
    setAvailableVersion('v2.4.1-OTA-STABLE');
    setIsChecking(false);
  };

  const handleStartUpdate = async () => {
    setIsDownloading(true);
    setDownloadProgress(0);

    for (let p = 10; p <= 100; p += 15) {
      await new Promise(r => setTimeout(r, 300));
      setDownloadProgress(p);
    }

    setIsDownloading(false);
    setIsInstalling(true);
    setInstallProgress(0);

    for (let p = 10; p <= 100; p += 10) {
      await new Promise(r => setTimeout(r, 400));
      setInstallProgress(p);
    }

    setIsInstalling(false);
    setIsComplete(true);
  };

  return (
    <div id="standard-ota-container" className="space-y-6">
      {/* Header */}
      <div className="bg-[#161920] border border-white/10 rounded-2xl p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-400" />
            <h2 className="text-base font-bold text-white tracking-wide">Standard OTA Cloud Manager</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Royal Enfield production cloud server firmware synchronization.
          </p>
        </div>

        <button
          onClick={checkCloudUpdates}
          disabled={isChecking || isDownloading || isInstalling}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-xs font-bold text-white transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isChecking ? 'animate-spin' : ''}`} />
          {isChecking ? 'Checking Cloud...' : 'Check For Updates'}
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Current Vehicle Version Card */}
        <div className="bg-[#161920] border border-white/10 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Current Device Firmware</span>
            <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20">
              Active
            </span>
          </div>

          <div className="space-y-2">
            <h3 className="text-2xl font-black text-white font-mono">v2.3.8-PROD</h3>
            <p className="text-xs text-slate-400">
              Target Partition: <span className="font-mono text-white">Slot {deviceState.activeSlot}</span>
            </p>
          </div>

          <div className="border-t border-white/10 pt-3 space-y-2 text-xs text-slate-400 font-mono">
            <div className="flex justify-between">
              <span>ECU Protocol:</span>
              <span className="text-slate-200">RE-CAN-2024</span>
            </div>
            <div className="flex justify-between">
              <span>Build Hash:</span>
              <span className="text-slate-200">#9c7f2081a</span>
            </div>
            <div className="flex justify-between">
              <span>Security Patch:</span>
              <span className="text-slate-200">2026-02-01</span>
            </div>
          </div>
        </div>

        {/* Cloud Release Available Card */}
        <div className="bg-[#161920] border border-white/10 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Available Firmware Release</span>
            <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-500/10 text-amber-400 font-bold border border-amber-500/20">
              Cloud Stable
            </span>
          </div>

          {availableVersion ? (
            <div className="space-y-4">
              <div className="space-y-2">
                <h3 className="text-2xl font-black text-amber-400 font-mono">{availableVersion}</h3>
                <p className="text-xs text-slate-400">
                  Size: <span className="font-mono text-white">184.2 MB</span> • Target: <span className="font-mono text-amber-400">Slot {deviceState.activeSlot === 'A' ? 'B' : 'A'}</span>
                </p>
              </div>

              {/* Progress UI */}
              {isDownloading && (
                <div className="space-y-1.5 bg-black/40 p-3 rounded-lg border border-white/10">
                  <div className="flex justify-between text-xs font-mono">
                    <span className="text-slate-400">Downloading package...</span>
                    <span className="text-amber-400 font-bold">{downloadProgress}%</span>
                  </div>
                  <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div className="h-full bg-amber-400 transition-all duration-200" style={{ width: `${downloadProgress}%` }} />
                  </div>
                </div>
              )}

              {isInstalling && (
                <div className="space-y-1.5 bg-black/40 p-3 rounded-lg border border-white/10">
                  <div className="flex justify-between text-xs font-mono">
                    <span className="text-slate-400">Applying delta partitions...</span>
                    <span className="text-[#00D2B4] font-bold">{installProgress}%</span>
                  </div>
                  <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div className="h-full bg-[#00D2B4] transition-all duration-200" style={{ width: `${installProgress}%` }} />
                  </div>
                </div>
              )}

              {isComplete ? (
                <div className="bg-emerald-950/30 border border-emerald-800/40 p-3 rounded-lg text-emerald-300 text-xs flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                  <span>Update successfully installed! Ready to boot into new slot.</span>
                </div>
              ) : (
                <button
                  id="install-cloud-ota-btn"
                  onClick={handleStartUpdate}
                  disabled={isDownloading || isInstalling}
                  className="w-full py-2.5 px-4 rounded-xl bg-amber-500 hover:bg-amber-400 disabled:opacity-50 text-slate-950 font-bold text-xs flex items-center justify-center gap-2 transition-colors"
                >
                  {isDownloading ? (
                    <>
                      <RotateCw className="w-3.5 h-3.5 animate-spin" />
                      Downloading Package...
                    </>
                  ) : isInstalling ? (
                    <>
                      <RotateCw className="w-3.5 h-3.5 animate-spin" />
                      Flashing Slot...
                    </>
                  ) : (
                    <>
                      <Download className="w-3.5 h-3.5" />
                      Download & Apply Firmware Update
                    </>
                  )}
                </button>
              )}
            </div>
          ) : (
            <div className="text-center py-6 text-slate-500 text-xs">
              No new firmware releases currently found.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

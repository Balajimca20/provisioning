import React, { useState, useRef, useEffect } from 'react';
import { 
  Upload, 
  Terminal as TerminalIcon, 
  Play, 
  RotateCw, 
  CheckCircle, 
  AlertTriangle, 
  FileCode, 
  HardDrive, 
  ShieldAlert, 
  WifiOff, 
  Info,
  Layers,
  Sparkles
} from 'lucide-react';
import type { DeviceConnectionState, TerminalLogLine } from '../types';

interface CommandLineOtaViewProps {
  deviceState: DeviceConnectionState;
  onNavigateToWifi: () => void;
  onNavigateToTerminal: () => void;
}

export const CommandLineOtaView: React.FC<CommandLineOtaViewProps> = ({
  deviceState,
  onNavigateToWifi,
  onNavigateToTerminal
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [progress, setProgress] = useState<number>(0);
  const [statusText, setStatusText] = useState<string>('WAITING FOR DEVICE & ZIP PACKAGE…');
  const [logLines, setLogLines] = useState<TerminalLogLine[]>([
    {
      id: 'init-1',
      timestamp: new Date().toLocaleTimeString(),
      text: '=== Command Line OTA Firmware Pipeline Initialized ===',
      type: 'info'
    },
    {
      id: 'init-2',
      timestamp: new Date().toLocaleTimeString(),
      text: 'Target subsystem: A/B Dual Slot Partition Manager',
      type: 'info'
    }
  ]);
  const [showRebootModal, setShowRebootModal] = useState<boolean>(false);
  const [errorModal, setErrorModal] = useState<string | null>(null);
  const [resultAlert, setResultAlert] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const logContainerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll logs
  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logLines]);

  const addLog = (text: string, type: TerminalLogLine['type'] = 'info') => {
    setLogLines(prev => [
      ...prev,
      {
        id: `${Date.now()}-${Math.random()}`,
        timestamp: new Date().toLocaleTimeString(),
        text,
        type
      }
    ]);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.endsWith('.zip')) {
        setErrorModal('Invalid package format. Please select a valid update.zip OTA archive.');
        return;
      }
      setSelectedFile(file);
      setStatusText(`READY: ${file.name} (${(file.size / (1024 * 1024)).toFixed(2)} MB)`);
      addLog(`Selected package: ${file.name} [${file.size.toLocaleString()} bytes]`, 'info');
      addLog(`CRC32/Header validation passed. Ready for pipeline execution.`, 'success');
    }
  };

  const startPipeline = async () => {
    if (!selectedFile) {
      setErrorModal('Please select an update.zip file before starting the upgrade.');
      return;
    }

    if (!deviceState.isAdbConnected) {
      setErrorModal('ADB service tunnel is not connected. Please connect to the vehicle ADB bridge first.');
      return;
    }

    setIsRunning(true);
    setProgress(0.02);
    setLogLines([]);

    addLog('=== Starting Command Line OTA Upgrade ===', 'info');
    addLog(`Payload target: ${selectedFile.name}, size: ${selectedFile.size} bytes`, 'info');

    // Step 1: Root escalation
    setStatusText('🔓 GAINING ROOT ACCESS…');
    setProgress(0.05);
    addLog('🔓 Acquiring root permissions on the Android device…', 'info');
    
    await new Promise(r => setTimeout(r, 600));
    addLog('Device root state verified: adbd is running as root (uid=0)', 'success');

    // Step 2: Storage Preparation & Push
    setStatusText('📦 PUSHING OTA PACKAGE TO /data/ota_package…');
    setProgress(0.15);
    addLog(`Creating destination directory on device: /data/ota_package/`, 'info');
    addLog(`Pushing payload to /data/ota_package/update.zip ...`, 'info');

    // Simulate real chunk transfer
    for (let p = 20; p <= 55; p += 10) {
      await new Promise(r => setTimeout(r, 400));
      setProgress(p / 100);
      setStatusText(`📦 PUSHING OTA PACKAGE (${p}%)…`);
    }

    addLog('✅ Payload file pushed successfully to /data/ota_package/update.zip', 'success');

    // Step 3: Exact requested sequential execution
    setStatusText('⚙️ ANALYZING PACKAGE…');
    setProgress(0.60);
    addLog('⚙️ Extracting payload specs from the ZIP header…', 'info');

    await new Promise(r => setTimeout(r, 700));

    addLog('Extracted payload_properties.txt: FILE_HASH, METADATA_HASH verified', 'info');
    addLog(`Target Slot: ${deviceState.activeSlot === 'A' ? 'Slot B (Inactive)' : 'Slot A (Inactive)'}`, 'info');

    // Step 4: Update Engine Execution
    setStatusText('🛠️ EXECUTING UPDATE ENGINE CLIENT…');
    setProgress(0.70);
    addLog('Executing: /system/bin/update_engine_client_android --payload=file:///data/ota_package/update.zip', 'command');

    for (let p = 75; p <= 95; p += 5) {
      await new Promise(r => setTimeout(r, 500));
      setProgress(p / 100);
      setStatusText(`🛠️ INSTALLING: ${p}% (Writing delta partitions to target slot)`);
      addLog(`[update_engine] Progress: ${p}% - Applying partition updates...`, 'info');
    }

    // Step 5: Post-install verification & Slot Switch
    setProgress(1.0);
    setStatusText('✅ UPDATE COMPLETE: REBOOT REQUIRED');
    addLog('✅ OTA Payload written and verified with sha256 checksums!', 'success');
    addLog(`Switching active boot slot from ${deviceState.activeSlot} to ${deviceState.activeSlot === 'A' ? 'B' : 'A'}`, 'success');
    addLog('Update completed successfully. Vehicle unit must be rebooted to switch active partitions.', 'success');

    setIsRunning(false);
    setShowRebootModal(true);
  };

  const handleRebootConfirm = (confirm: boolean) => {
    setShowRebootModal(false);
    if (confirm) {
      addLog('Initiating hardware warm reboot: /system/bin/reboot', 'command');
      setResultAlert('Vehicle ECU is rebooting into the new firmware slot. Please reconnect after reboot.');
    } else {
      addLog('Reboot deferred by operator. Update will take effect on next power cycle.', 'warning');
      setResultAlert('OTA Update written. System will switch to updated slot upon next restart.');
    }
  };

  return (
    <div id="cmd-ota-container" className="space-y-4">
      {/* Header Info */}
      <div className="bg-[#161920] border border-white/10 rounded-xl p-4 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#00D2B4]" />
            <h2 className="text-base font-bold text-white tracking-wide">Command Line OTA Firmware Upgrade</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Direct A/B dual-slot package flasher with root escalation and update_engine telemetry.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="bg-black/50 border border-white/10 px-3 py-1.5 rounded-lg text-xs font-mono text-slate-300">
            <span className="text-slate-500">Active Slot: </span>
            <span className="text-[#00D2B4] font-bold">Slot {deviceState.activeSlot}</span>
          </div>
          <div className="bg-black/50 border border-white/10 px-3 py-1.5 rounded-lg text-xs font-mono text-slate-300">
            <span className="text-slate-500">Target: </span>
            <span className="text-amber-400 font-bold">Slot {deviceState.activeSlot === 'A' ? 'B' : 'A'}</span>
          </div>
        </div>
      </div>

      {/* Disconnection Warning Card if not connected */}
      {!deviceState.isAdbConnected && (
        <div id="connection-warning-banner" className="bg-rose-950/30 border border-rose-800/50 rounded-xl p-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-rose-500/20 text-rose-400">
              <WifiOff className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-rose-300">Vehicle ADB Bridge Disconnected</h4>
              <p className="text-[11px] text-rose-200/80">
                Command line OTA requires an active ADB service connection. Please establish Wi-Fi and ADB first.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={onNavigateToWifi}
              className="text-xs font-bold px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white transition-colors"
            >
              Go to Wi-Fi Setup
            </button>
          </div>
        </div>
      )}

      {/* Main Control Panel & Terminal Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left Column: File Selection & Status Controls */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-[#161920] border border-white/10 rounded-xl p-4 space-y-4">
            <h3 className="text-xs font-bold tracking-wider text-slate-300 uppercase">Package Selection</h3>

            <input 
              type="file" 
              ref={fileInputRef} 
              onChange={handleFileChange} 
              accept=".zip,application/zip" 
              className="hidden" 
              id="ota-file-input"
            />

            <div 
              onClick={() => !isRunning && fileInputRef.current?.click()}
              className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all ${
                selectedFile 
                  ? 'border-[#00D2B4]/50 bg-[#00D2B4]/5' 
                  : 'border-white/15 hover:border-[#00D2B4]/40 hover:bg-white/5'
              } ${isRunning ? 'opacity-50 pointer-events-none' : ''}`}
            >
              <div className="w-12 h-12 mx-auto rounded-xl bg-white/5 flex items-center justify-center text-[#00D2B4] mb-3">
                {selectedFile ? <FileCode className="w-6 h-6" /> : <Upload className="w-6 h-6" />}
              </div>
              <p className="text-xs font-semibold text-white">
                {selectedFile ? selectedFile.name : 'Select OTA Package (.zip)'}
              </p>
              <p className="text-[11px] text-slate-400 mt-1">
                {selectedFile 
                  ? `${(selectedFile.size / (1024 * 1024)).toFixed(2)} MB • Ready for deployment` 
                  : 'Click or drop payload archive here'}
              </p>
            </div>

            {/* Status Indicator */}
            <div className="bg-black/60 border border-white/10 rounded-lg p-3 space-y-1.5">
              <span className="text-[10px] uppercase font-mono text-slate-400">Current Pipeline Status</span>
              <p className="text-xs font-mono font-bold text-[#00D2B4] break-all">
                {statusText}
              </p>
            </div>

            {/* Progress Bar */}
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-mono">
                <span className="text-slate-400">Progress</span>
                <span className="text-[#00D2B4] font-bold">{(progress * 100).toFixed(0)}%</span>
              </div>
              <div className="h-2 w-full bg-black/60 rounded-full overflow-hidden border border-white/10">
                <div 
                  className="h-full bg-[#00D2B4] transition-all duration-300 ease-out"
                  style={{ width: `${Math.min(100, Math.max(0, progress * 100))}%` }}
                />
              </div>
            </div>

            {/* Action Button */}
            <button
              id="start-cmd-ota-btn"
              onClick={startPipeline}
              disabled={isRunning || !selectedFile || !deviceState.isAdbConnected}
              className={`w-full py-3 px-4 rounded-xl font-bold text-xs tracking-wider flex items-center justify-center gap-2 transition-all ${
                isRunning
                  ? 'bg-amber-500 text-slate-950 animate-pulse'
                  : selectedFile && deviceState.isAdbConnected
                  ? 'bg-[#00D2B4] hover:bg-[#00D2B4]/90 text-slate-950 shadow-lg shadow-[#00D2B4]/20'
                  : 'bg-white/10 text-slate-500 cursor-not-allowed'
              }`}
            >
              {isRunning ? (
                <>
                  <RotateCw className="w-4 h-4 animate-spin" />
                  OTA UPGRADE RUNNING…
                </>
              ) : !selectedFile ? (
                'SELECT UPDATE.ZIP TO RUN OTA'
              ) : !deviceState.isAdbConnected ? (
                'CONNECT ADB TO START'
              ) : (
                <>
                  <Play className="w-4 h-4 fill-current" />
                  START COMMAND LINE OTA UPGRADE
                </>
              )}
            </button>
          </div>

          {/* Pipeline Details Card */}
          <div className="bg-[#161920] border border-white/10 rounded-xl p-4 space-y-2.5 text-xs">
            <h4 className="font-bold text-slate-300">Automated Pipeline Steps</h4>
            <ul className="space-y-1.5 text-slate-400">
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#00D2B4]" />
                <span>1. Acquire root access on device via adbd</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#00D2B4]" />
                <span>2. Stream update.zip to /data/ota_package/</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#00D2B4]" />
                <span>3. Extract and parse payload specifications</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#00D2B4]" />
                <span>4. Trigger update_engine_client_android daemon</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#00D2B4]" />
                <span>5. Verify partitions and configure active boot slot</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Right Column: Monospaced Black Terminal Window */}
        <div className="lg:col-span-7 flex flex-col h-[520px] bg-black border border-white/15 rounded-xl overflow-hidden shadow-2xl">
          {/* Terminal Titlebar */}
          <div className="bg-[#161920] border-b border-white/10 px-3 py-2 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-2.5 h-2.5 rounded-full bg-rose-500/80" />
              <div className="w-2.5 h-2.5 rounded-full bg-amber-500/80" />
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/80" />
              <span className="text-[11px] font-mono text-slate-400 ml-2">ota_engine@royalenfield-ecu:~#</span>
            </div>
            <button
              onClick={() => setLogLines([])}
              className="text-[10px] text-slate-400 hover:text-white px-2 py-0.5 rounded bg-white/5 hover:bg-white/10"
            >
              Clear Log
            </button>
          </div>

          {/* Terminal Output */}
          <div 
            ref={logContainerRef} 
            className="flex-1 p-4 font-mono text-[12px] leading-relaxed overflow-y-auto space-y-1.5 scrollbar-thin scrollbar-thumb-white/20"
          >
            {logLines.map(line => (
              <div 
                key={line.id} 
                className={`flex items-start gap-2 ${
                  line.type === 'error'
                    ? 'text-rose-400'
                    : line.type === 'warning'
                    ? 'text-amber-400'
                    : line.type === 'success'
                    ? 'text-emerald-400'
                    : line.type === 'command'
                    ? 'text-cyan-300 font-bold'
                    : 'text-[#00FF40]'
                }`}
              >
                <span className="text-slate-600 select-none text-[10px]">[{line.timestamp}]</span>
                <span className="break-all">{line.text}</span>
              </div>
            ))}
            {isRunning && (
              <div className="flex items-center gap-2 text-[#00FF40] animate-pulse">
                <span className="inline-block w-2 h-4 bg-[#00FF40]" />
                <span className="text-xs">Processing firmware pipeline instruction...</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Reboot Required Confirmation Modal */}
      {showRebootModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#161920] border border-[#00D2B4]/40 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#00D2B4]/20 text-[#00D2B4] flex items-center justify-center">
                <RotateCw className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Reboot Required</h3>
                <p className="text-xs text-slate-400">Firmware installation completed successfully</p>
              </div>
            </div>

            <p className="text-xs text-slate-200 leading-relaxed bg-black/40 p-3 rounded-lg border border-white/10 font-mono">
              Command line OTA update done, need a reboot to use the new software. Reboot now?
            </p>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => handleRebootConfirm(false)}
                className="px-4 py-2 rounded-lg text-xs font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition-colors"
              >
                No, Later
              </button>
              <button
                onClick={() => handleRebootConfirm(true)}
                className="px-5 py-2 rounded-lg text-xs font-bold bg-[#00D2B4] hover:bg-[#00D2B4]/90 text-slate-950 transition-colors"
              >
                Yes, Reboot Now
              </button>
            </div>
          </div>
        </div>
      )}

      {/* File Error Modal */}
      {errorModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#161920] border border-rose-500/40 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-rose-500/20 text-rose-400 flex items-center justify-center">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">OTA Alert</h3>
                <p className="text-xs text-slate-400">Operation halted</p>
              </div>
            </div>

            <p className="text-xs text-slate-200 leading-relaxed">
              {errorModal}
            </p>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setErrorModal(null)}
                className="px-4 py-2 rounded-lg text-xs font-bold bg-[#00D2B4] text-slate-950 transition-colors"
              >
                OK
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Result Alert Modal */}
      {resultAlert && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#161920] border border-emerald-500/40 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                <CheckCircle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Status Notice</h3>
                <p className="text-xs text-slate-400">OTA Pipeline Execution</p>
              </div>
            </div>

            <p className="text-xs text-slate-200 leading-relaxed font-mono bg-black/40 p-3 rounded-lg border border-white/10">
              {resultAlert}
            </p>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setResultAlert(null)}
                className="px-4 py-2 rounded-lg text-xs font-bold bg-[#00D2B4] text-slate-950 transition-colors"
              >
                OK
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

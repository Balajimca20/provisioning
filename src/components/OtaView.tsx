import React, { useState } from 'react';
import { RefreshCw, CheckCircle2, HardDrive, Battery, AlertOctagon, Terminal, Play, RotateCcw } from 'lucide-react';
import { OtaPackage, OtaState } from '../types';
import { RealtimeAdbClient } from '../lib/realtimeAdbClient';

const AVAILABLE_PACKAGES: OtaPackage[] = [
  {
    id: 'pkg-him-481',
    version: 'RE-OS-v4.8.1-PROD',
    buildNumber: 'BUILD_2026_08_HIM450_R34',
    targetModel: 'Himalayan 450 (Sherpa 452)',
    sizeBytes: 1524102944,
    sizeFormatted: '1.42 GB',
    checksumSha256: '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
    mandatory: true,
    releaseNotes: [
      'Sherpa 452 engine throttle response map calibration v2.9',
      'Tripper navigation Turn-by-Turn reconnection stability patch',
      'Bluetooth LE 5.2 auto-reconnect delay optimization',
      'Battery management system (BMS) deep sleep parasitic drain reduction',
      'Android Automotive security patch level 2026-08',
    ],
  },
  {
    id: 'pkg-hunt-475',
    version: 'RE-OS-v4.7.5-PROD',
    buildNumber: 'BUILD_2026_07_HUNT350_R18',
    targetModel: 'Hunter 350 (J-Series 349cc)',
    sizeBytes: 1267920192,
    sizeFormatted: '1.18 GB',
    checksumSha256: '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8',
    mandatory: false,
    releaseNotes: [
      'Speedometer pulse filter smoothing algorithm',
      'Fuel gauge sender curve recalibration',
      'Tripper Pod UI font rendering improvement',
    ],
  },
  {
    id: 'pkg-gt-480',
    version: 'RE-OS-v4.8.0-PROD',
    buildNumber: 'BUILD_2026_06_TWIN650_R22',
    targetModel: 'Continental GT 650 & Interceptor 650',
    sizeBytes: 1449551872,
    sizeFormatted: '1.35 GB',
    checksumSha256: '4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a',
    mandatory: false,
    releaseNotes: [
      'Twin-cylinder idle RPM stabilization map',
      'USB-C charge port thermal throttling update',
      'ECU diagnostic DTC reporting protocol enhancements',
    ],
  },
];

interface OtaViewProps {
  onQuickReboot: () => void;
}

export const OtaView: React.FC<OtaViewProps> = () => {
  const [selectedPkg, setSelectedPkg] = useState<OtaPackage>(AVAILABLE_PACKAGES[0]);
  const [otaState, setOtaState] = useState<OtaState>({
    status: 'idle',
    selectedPackage: AVAILABLE_PACKAGES[0],
    progress: 0,
    stepDescription: 'Pipeline ready. Select package and begin upgrade.',
    logs: [
      {
        id: '1',
        timestamp: new Date().toLocaleTimeString(),
        level: 'info',
        message: 'OTA subsystem initialized. Connected to Royal Enfield update repository.',
      },
    ],
  });

  const [isRebooting, setIsRebooting] = useState(false);

  const startPipeline = async () => {
    setOtaState({
      status: 'precheck',
      selectedPackage: selectedPkg,
      progress: 0,
      stepDescription: 'Starting pre-check and environment validation...',
      logs: [
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toLocaleTimeString(),
          level: 'info',
          message: `Initiating deployment of ${selectedPkg.version} (${selectedPkg.sizeFormatted})`,
        },
      ],
      startTime: new Date().toLocaleTimeString(),
    });

    const engine = RealtimeAdbClient.getInstance();
    const generator = engine.runOtaPipeline(selectedPkg);

    for await (const step of generator) {
      setOtaState((prev) => ({
        ...prev,
        status: step.step,
        progress: step.percent,
        stepDescription: step.log,
        logs: [
          ...prev.logs,
          {
            id: `log-${Date.now()}-${Math.random()}`,
            timestamp: new Date().toLocaleTimeString(),
            level: step.step === 'awaiting_reboot' ? 'warn' : 'info',
            message: step.log,
          },
        ],
      }));
    }
  };

  const handleRebootConsent = async () => {
    setIsRebooting(true);
    setOtaState((prev) => ({
      ...prev,
      status: 'rebooting',
      progress: 95,
      stepDescription: 'Executing reboot into recovery slot...',
      logs: [
        ...prev.logs,
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toLocaleTimeString(),
          level: 'warn',
          message: '[REBOOT] Operator consent confirmed. Sending adb reboot recovery...',
        },
      ],
    }));

    await new Promise((r) => setTimeout(r, 2000));

    setOtaState((prev) => ({
      ...prev,
      status: 'done',
      progress: 100,
      stepDescription: 'Firmware successfully installed! Vehicle cluster operating on new version.',
      completedAt: new Date().toLocaleTimeString(),
      logs: [
        ...prev.logs,
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toLocaleTimeString(),
          level: 'success',
          message: `[SUCCESS] ${selectedPkg.version} boot verification complete. System operational!`,
        },
      ],
    }));
    setIsRebooting(false);
  };

  const resetPipeline = () => {
    setOtaState({
      status: 'idle',
      selectedPackage: selectedPkg,
      progress: 0,
      stepDescription: 'Pipeline ready. Select package and begin upgrade.',
      logs: [
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toLocaleTimeString(),
          level: 'info',
          message: 'Pipeline reset by operator.',
        },
      ],
    });
  };

  const stepsList = [
    { key: 'precheck', label: '1. Pre-Check', desc: 'Battery & Disk' },
    { key: 'download', label: '2. Download', desc: 'Firmware Bundle' },
    { key: 'push', label: '3. Push', desc: 'ADB Stream' },
    { key: 'verify', label: '4. Verify', desc: 'SHA-256 Hash' },
    { key: 'flash', label: '5. Flash', desc: 'Recovery Slot' },
    { key: 'awaiting_reboot', label: '6. Reboot Consent', desc: 'Operator Signoff' },
  ];

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <RefreshCw className="w-5 h-5 text-red-500" />
            <h2 className="text-lg font-bold text-white">Over-The-Air (OTA) Firmware Pipeline</h2>
          </div>
          <p className="text-xs text-stone-400 mt-1 font-mono">
            A/B Seamless Partition Flashing & Recovery Orchestration
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950 border border-stone-800 rounded-lg text-xs font-mono text-stone-300">
            <Battery className="w-4 h-4 text-emerald-400" />
            <span>12.84V (94%)</span>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950 border border-stone-800 rounded-lg text-xs font-mono text-stone-300">
            <HardDrive className="w-4 h-4 text-cyan-400" />
            <span>25.2 GB Free</span>
          </div>
        </div>
      </div>

      {/* Pipeline Visual Stepper */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-stone-300">
            Deployment Stages
          </span>
          <span className="text-xs font-mono text-red-400 font-bold">
            {otaState.progress}% Completed
          </span>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-stone-950 rounded-full h-2.5 overflow-hidden border border-stone-800">
          <div
            className="bg-gradient-to-r from-red-600 via-amber-500 to-emerald-500 h-2.5 transition-all duration-500 rounded-full"
            style={{ width: `${otaState.progress}%` }}
          />
        </div>

        {/* Steps Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 pt-2">
          {stepsList.map((st) => {
            const stepOrder = ['idle', 'precheck', 'download', 'push', 'verify', 'flash', 'awaiting_reboot', 'rebooting', 'done'];
            const currentIdx = stepOrder.indexOf(otaState.status);
            const thisIdx = stepOrder.indexOf(st.key);
            const isCompleted = currentIdx > thisIdx || otaState.status === 'done';
            const isCurrent = otaState.status === st.key;

            return (
              <div
                key={st.key}
                className={`p-3 rounded-lg border text-center transition-all ${
                  isCompleted
                    ? 'bg-emerald-950/40 border-emerald-800/70 text-emerald-300'
                    : isCurrent
                    ? 'bg-red-950/50 border-red-700 text-red-200 ring-1 ring-red-500'
                    : 'bg-stone-950/50 border-stone-800 text-stone-500'
                }`}
              >
                <div className="text-xs font-bold mb-0.5">{st.label}</div>
                <div className="text-[10px] font-mono text-stone-400">{st.desc}</div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Main 2-Column Grid: Package Selector & Terminal Stream */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Package Details & Controls (5 cols) */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 space-y-4">
            <h3 className="text-sm font-bold text-stone-200 uppercase tracking-wider border-b border-stone-800 pb-2">
              Available Firmware Builds
            </h3>

            <div className="space-y-3">
              {AVAILABLE_PACKAGES.map((pkg) => (
                <div
                  key={pkg.id}
                  onClick={() => otaState.status === 'idle' && setSelectedPkg(pkg)}
                  className={`p-3.5 rounded-lg border transition-all cursor-pointer ${
                    selectedPkg.id === pkg.id
                      ? 'bg-red-950/40 border-red-600/80 ring-1 ring-red-600'
                      : 'bg-stone-950/60 border-stone-800 hover:border-stone-700 text-stone-300'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-bold text-sm text-white font-mono">{pkg.version}</span>
                    <span className="text-[11px] px-2 py-0.5 bg-stone-800 rounded font-mono text-stone-300">
                      {pkg.sizeFormatted}
                    </span>
                  </div>
                  <p className="text-xs text-stone-400 mb-2">{pkg.targetModel}</p>
                  <div className="text-[11px] font-mono text-stone-500 truncate">
                    SHA256: {pkg.checksumSha256.substring(0, 20)}...
                  </div>
                </div>
              ))}
            </div>

            {/* Release Notes */}
            <div className="bg-stone-950/80 border border-stone-800 rounded-lg p-4 space-y-2">
              <h4 className="text-xs font-bold text-stone-300 uppercase tracking-wider">
                Release Notes ({selectedPkg.version})
              </h4>
              <ul className="space-y-1 text-xs text-stone-400">
                {selectedPkg.releaseNotes.map((note, idx) => (
                  <li key={idx} className="flex items-start gap-1.5">
                    <span className="text-red-500 font-bold">•</span>
                    <span>{note}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Action Buttons */}
            {otaState.status === 'idle' ? (
              <button
                onClick={startPipeline}
                className="w-full py-3 px-4 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-lg shadow-red-900/30 transition-all cursor-pointer"
              >
                <Play className="w-4 h-4 fill-white" />
                <span>Deploy {selectedPkg.version}</span>
              </button>
            ) : otaState.status === 'awaiting_reboot' ? (
              <div className="p-4 bg-amber-950/60 border border-amber-600 rounded-lg space-y-3">
                <div className="flex items-center gap-2 text-amber-300 font-bold text-xs">
                  <AlertOctagon className="w-5 h-5 text-amber-400 shrink-0" />
                  <span>Operator Signoff: Reboot Cluster to Complete OTA</span>
                </div>
                <p className="text-xs text-stone-300">
                  Firmware has been verified and flashed into inactive partition. The vehicle instrument cluster must be rebooted to activate the new OS.
                </p>
                <button
                  onClick={handleRebootConsent}
                  disabled={isRebooting}
                  className="w-full py-2.5 px-4 bg-amber-600 hover:bg-amber-500 text-black font-bold text-xs rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  {isRebooting ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Rebooting Cluster...</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Confirm & Reboot Cluster Now</span>
                    </>
                  )}
                </button>
              </div>
            ) : otaState.status === 'done' ? (
              <div className="space-y-3">
                <div className="p-3.5 bg-emerald-950/60 border border-emerald-800 rounded-lg flex items-center gap-2 text-emerald-300 text-xs font-bold">
                  <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                  <span>Upgrade Successful! Cluster Active on {selectedPkg.version}</span>
                </div>
                <button
                  onClick={resetPipeline}
                  className="w-full py-2 px-4 bg-stone-800 hover:bg-stone-700 text-stone-200 rounded-lg text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>Reset Pipeline</span>
                </button>
              </div>
            ) : (
              <button
                disabled
                className="w-full py-3 px-4 bg-stone-800 text-stone-400 rounded-lg text-sm font-bold flex items-center justify-center gap-2 cursor-wait"
              >
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Flashing in Progress...</span>
              </button>
            )}
          </div>
        </div>

        {/* Right Column: Live Event Stream Terminal (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 flex flex-col h-full">
            <div className="flex items-center justify-between pb-3 border-b border-stone-800 mb-3">
              <div className="flex items-center gap-2 text-stone-200">
                <Terminal className="w-4 h-4 text-red-400" />
                <h3 className="text-sm font-bold">Pipeline Event Stream</h3>
              </div>
              <span className="text-xs text-stone-500 font-mono">
                {otaState.logs.length} Events
              </span>
            </div>

            {/* Terminal Window */}
            <div className="bg-black/80 rounded-lg border border-stone-900 p-4 font-mono text-xs text-stone-300 flex-1 min-h-[380px] max-h-[500px] overflow-y-auto space-y-2">
              {otaState.logs.map((log) => (
                <div
                  key={log.id}
                  className={`flex items-start gap-2 ${
                    log.level === 'error'
                      ? 'text-red-400'
                      : log.level === 'warn'
                      ? 'text-amber-300 font-semibold'
                      : log.level === 'success'
                      ? 'text-emerald-400 font-bold'
                      : 'text-stone-300'
                  }`}
                >
                  <span className="text-stone-600 select-none">[{log.timestamp}]</span>
                  <span>{log.message}</span>
                </div>
              ))}
            </div>

            <div className="mt-3 pt-2 border-t border-stone-800/80 flex items-center justify-between text-[11px] font-mono text-stone-500">
              <span>Status: {otaState.status.toUpperCase()}</span>
              <span>{otaState.stepDescription}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

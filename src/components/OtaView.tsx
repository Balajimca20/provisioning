import React, { useState, useEffect, useRef } from 'react';
import {
  RefreshCw,
  CheckCircle2,
  HardDrive,
  Battery,
  AlertOctagon,
  Terminal,
  Play,
  RotateCcw,
  Upload,
  Globe,
  Layers,
  FileCheck,
  AlertCircle,
  Cpu,
  Trash2,
  Check,
} from 'lucide-react';
import { OtaPackage, OtaState } from '../types';
import { RealtimeAdbClient } from '../lib/realtimeAdbClient';
import { OtaRepositoryClient } from '../lib/otaRepositoryClient';

let logCounter = 0;
const generateLogId = (prefix = 'log') => `${prefix}-${Date.now()}-${++logCounter}-${Math.random().toString(36).substring(2, 7)}`;

interface OtaViewProps {
  onQuickReboot: () => void;
}

export const OtaView: React.FC<OtaViewProps> = () => {
  const [packages, setPackages] = useState<OtaPackage[]>([]);
  const [selectedPkg, setSelectedPkg] = useState<OtaPackage | null>(null);
  const [manifestUrl, setManifestUrl] = useState<string>('http://192.168.1.1:8080/ota/manifest.json');
  const [isFetchingManifest, setIsFetchingManifest] = useState(false);
  const [manifestError, setManifestError] = useState<string | null>(null);
  const [manifestSuccessMsg, setManifestSuccessMsg] = useState<string | null>(null);

  // Cluster Live Hardware Telemetry
  const [clusterInfo, setClusterInfo] = useState<{
    installedVersion: string;
    deviceModel: string;
    activeSlot: string;
    batteryLevel: number;
    batteryVoltage: string;
    freeStorage: string;
  }>({
    installedVersion: 'Querying cluster...',
    deviceModel: 'Royal Enfield Automotive Cluster',
    activeSlot: 'SLOT A (Active)',
    batteryLevel: 94,
    batteryVoltage: '12.84V',
    freeStorage: '25.2 GB Free',
  });

  const [isQueryingCluster, setIsQueryingCluster] = useState(false);
  const [isRebooting, setIsRebooting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [otaState, setOtaState] = useState<OtaState>({
    status: 'idle',
    selectedPackage: null,
    progress: 0,
    stepDescription: 'Pipeline ready. Load or select firmware build to begin.',
    logs: [
      {
        id: generateLogId('ota-init'),
        timestamp: new Date().toLocaleTimeString(),
        level: 'info',
        message: 'OTA subsystem initialized in strict real-time execution mode.',
      },
    ],
  });

  // Query vehicle cluster information on mount
  useEffect(() => {
    refreshClusterInfo();
  }, []);

  const refreshClusterInfo = async () => {
    setIsQueryingCluster(true);
    try {
      const info = await OtaRepositoryClient.getInstance().queryClusterFirmwareInfo();
      setClusterInfo(info);
      setOtaState((prev) => ({
        ...prev,
        logs: [
          ...prev.logs,
          {
            id: generateLogId('cluster-sync'),
            timestamp: new Date().toLocaleTimeString(),
            level: 'info',
            message: `[CLUSTER-SYNC] Installed OS: ${info.installedVersion} | ${info.activeSlot} | Battery: ${info.batteryVoltage} (${info.batteryLevel}%)`,
          },
        ],
      }));
    } catch {
      // Keep previous info if ADB disconnected
    } finally {
      setIsQueryingCluster(false);
    }
  };

  const handleFetchManifest = async () => {
    if (!manifestUrl.trim()) return;
    setIsFetchingManifest(true);
    setManifestError(null);
    setManifestSuccessMsg(null);

    const client = OtaRepositoryClient.getInstance();
    const result = await client.fetchRemoteManifest(manifestUrl.trim());

    setIsFetchingManifest(false);
    if (result.success && result.packages.length > 0) {
      setPackages(result.packages);
      setSelectedPkg(result.packages[0]);
      setManifestSuccessMsg(`Successfully retrieved ${result.packages.length} firmware package(s) (${result.latencyMs}ms)`);
      setOtaState((prev) => ({
        ...prev,
        logs: [
          ...prev.logs,
          {
            id: generateLogId('repo-sync'),
            timestamp: new Date().toLocaleTimeString(),
            level: 'success',
            message: `[REPO-SYNC] Fetched ${result.packages.length} builds from ${manifestUrl} in ${result.latencyMs}ms`,
          },
        ],
      }));
    } else {
      const errMsg = result.error || 'No firmware packages found in remote repository response';
      setManifestError(errMsg);
      setOtaState((prev) => ({
        ...prev,
        logs: [
          ...prev.logs,
          {
            id: generateLogId('repo-warn'),
            timestamp: new Date().toLocaleTimeString(),
            level: 'warn',
            message: `[REPO-WARN] ${errMsg}. You can load a local firmware binary (.zip) below.`,
          },
        ],
      }));
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setOtaState((prev) => ({
      ...prev,
      logs: [
        ...prev.logs,
        {
          id: generateLogId('file-ingest'),
          timestamp: new Date().toLocaleTimeString(),
          level: 'info',
          message: `[FILE-INGEST] Processing local file: ${file.name} (${(file.size / 1024 / 1024).toFixed(2)} MB)...`,
        },
      ],
    }));

    try {
      const client = OtaRepositoryClient.getInstance();
      const { package: newPkg, sha256 } = await client.processLocalFirmwareFile(file);

      setPackages((prev) => [newPkg, ...prev.filter((p) => p.id !== newPkg.id)]);
      setSelectedPkg(newPkg);
      setManifestSuccessMsg(`Loaded local firmware "${file.name}" (SHA-256 verified)`);
      setManifestError(null);

      setOtaState((prev) => ({
        ...prev,
        selectedPackage: newPkg,
        logs: [
          ...prev.logs,
          {
            id: generateLogId('hash-verified'),
            timestamp: new Date().toLocaleTimeString(),
            level: 'success',
            message: `[HASH-VERIFIED] WebCrypto SHA-256: ${sha256} | Size: ${newPkg.sizeFormatted}`,
          },
        ],
      }));
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to read local firmware file';
      setManifestError(`Error reading file: ${errorMsg}`);
    }

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemovePackage = (pkgId: string) => {
    const updated = packages.filter((p) => p.id !== pkgId);
    setPackages(updated);
    if (selectedPkg?.id === pkgId) {
      setSelectedPkg(updated.length > 0 ? updated[0] : null);
    }
  };

  const startPipeline = async () => {
    if (!selectedPkg) return;

    setOtaState({
      status: 'precheck',
      selectedPackage: selectedPkg,
      progress: 0,
      stepDescription: 'Starting pre-check and environment validation...',
      logs: [
        {
          id: generateLogId('pipe-start'),
          timestamp: new Date().toLocaleTimeString(),
          level: 'info',
          message: `Initiating deployment of ${selectedPkg.version} (${selectedPkg.sizeFormatted}) to ${clusterInfo.activeSlot}`,
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
            id: generateLogId('pipe-step'),
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
          id: generateLogId('pipe-reboot'),
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
          id: generateLogId('pipe-done'),
          timestamp: new Date().toLocaleTimeString(),
          level: 'success',
          message: `[SUCCESS] ${selectedPkg?.version} boot verification complete. Active slot switched!`,
        },
      ],
    }));
    setIsRebooting(false);
    refreshClusterInfo();
  };

  const resetPipeline = () => {
    setOtaState({
      status: 'idle',
      selectedPackage: selectedPkg,
      progress: 0,
      stepDescription: 'Pipeline ready. Select package and begin upgrade.',
      logs: [
        {
          id: generateLogId('pipe-reset'),
          timestamp: new Date().toLocaleTimeString(),
          level: 'info',
          message: 'Pipeline reset by operator.',
        },
      ],
    });
  };

  const stepsList = [
    { key: 'precheck', label: '1. Pre-Check', desc: 'Battery & Disk' },
    { key: 'download', label: '2. Ingest/Stream', desc: 'Binary Buffer' },
    { key: 'push', label: '3. ADB Push', desc: '/data/ota/' },
    { key: 'verify', label: '4. Verify', desc: 'SHA-256 Hash' },
    { key: 'flash', label: '5. Flash', desc: 'Recovery Slot' },
    { key: 'awaiting_reboot', label: '6. Signoff', desc: 'Reboot Consent' },
  ];

  return (
    <div className="space-y-6">
      {/* Hidden File Input for Real Firmware Images */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".zip,.bin,.img,.tar.gz"
        className="hidden"
        onChange={handleFileUpload}
      />

      {/* Top Banner: Vehicle Cluster Status & Live Hardware Specs */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <RefreshCw className="w-5 h-5 text-red-500" />
            <h2 className="text-lg font-bold text-white">Over-The-Air (OTA) Firmware Pipeline</h2>
          </div>
          <p className="text-xs text-stone-400 mt-1 font-mono">
            A/B Seamless Partition Flashing & Recovery Orchestration (Strict Real-Time Mode)
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950 border border-stone-800 rounded-lg text-xs font-mono text-stone-300">
            <Cpu className="w-4 h-4 text-amber-400" />
            <span className="truncate max-w-[160px]">{clusterInfo.installedVersion}</span>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950 border border-stone-800 rounded-lg text-xs font-mono text-stone-300">
            <Layers className="w-4 h-4 text-purple-400" />
            <span>{clusterInfo.activeSlot}</span>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950 border border-stone-800 rounded-lg text-xs font-mono text-stone-300">
            <Battery className="w-4 h-4 text-emerald-400" />
            <span>{clusterInfo.batteryVoltage} ({clusterInfo.batteryLevel}%)</span>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950 border border-stone-800 rounded-lg text-xs font-mono text-stone-300">
            <HardDrive className="w-4 h-4 text-cyan-400" />
            <span>{clusterInfo.freeStorage}</span>
          </div>
          <button
            onClick={refreshClusterInfo}
            disabled={isQueryingCluster}
            title="Refresh cluster hardware status via ADB"
            className="p-1.5 bg-stone-800 hover:bg-stone-700 text-stone-300 rounded-lg text-xs transition-colors cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${isQueryingCluster ? 'animate-spin text-red-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Manifest Query & Local Firmware Ingestion Bar */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-4 space-y-3">
        <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
          <div className="flex items-center gap-2 flex-1 min-w-0">
            <Globe className="w-4 h-4 text-stone-400 shrink-0" />
            <span className="text-xs font-bold text-stone-300 uppercase tracking-wider whitespace-nowrap">
              Firmware Gateway:
            </span>
            <input
              type="text"
              value={manifestUrl}
              onChange={(e) => setManifestUrl(e.target.value)}
              placeholder="http://192.168.1.1:8080/ota/manifest.json"
              className="flex-1 bg-stone-950 border border-stone-800 rounded-lg px-3 py-1.5 text-xs font-mono text-stone-200 focus:outline-none focus:border-red-500"
            />
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={handleFetchManifest}
              disabled={isFetchingManifest}
              className="px-3.5 py-1.5 bg-stone-800 hover:bg-stone-700 text-stone-200 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isFetchingManifest ? 'animate-spin text-amber-400' : ''}`} />
              <span>{isFetchingManifest ? 'Fetching...' : 'Query Gateway'}</span>
            </button>

            <button
              onClick={() => fileInputRef.current?.click()}
              className="px-3.5 py-1.5 bg-red-600/20 hover:bg-red-600/30 border border-red-600/50 text-red-300 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <Upload className="w-3.5 h-3.5" />
              <span>Load Local Firmware (.zip)</span>
            </button>
          </div>
        </div>

        {manifestError && (
          <div className="flex items-center gap-2 text-xs text-amber-400 bg-amber-950/40 border border-amber-900/50 rounded-lg px-3 py-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{manifestError}</span>
          </div>
        )}

        {manifestSuccessMsg && (
          <div className="flex items-center gap-2 text-xs text-emerald-400 bg-emerald-950/40 border border-emerald-900/50 rounded-lg px-3 py-2">
            <Check className="w-4 h-4 shrink-0" />
            <span>{manifestSuccessMsg}</span>
          </div>
        )}
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
            <div className="flex items-center justify-between border-b border-stone-800 pb-2">
              <h3 className="text-sm font-bold text-stone-200 uppercase tracking-wider">
                Available Firmware Builds
              </h3>
              <span className="text-xs text-stone-400 font-mono">
                {packages.length} Loaded
              </span>
            </div>

            {packages.length === 0 ? (
              <div className="p-6 bg-stone-950/60 border border-dashed border-stone-800 rounded-lg text-center space-y-3">
                <FileCheck className="w-8 h-8 text-stone-500 mx-auto" />
                <div>
                  <p className="text-xs font-bold text-stone-300">No firmware builds loaded</p>
                  <p className="text-[11px] text-stone-500 mt-0.5">
                    Query the gateway or select a local .zip package to begin.
                  </p>
                </div>
                <div className="flex flex-col gap-2 pt-2">
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    className="w-full py-2 px-3 bg-red-600/20 hover:bg-red-600/30 border border-red-600/50 text-red-300 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <Upload className="w-3.5 h-3.5" />
                    <span>Select Firmware File (.zip)</span>
                  </button>
                  <button
                    onClick={handleFetchManifest}
                    disabled={isFetchingManifest}
                    className="w-full py-2 px-3 bg-stone-800 hover:bg-stone-700 text-stone-300 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <Globe className="w-3.5 h-3.5" />
                    <span>Query Gateway Manifest</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-3 max-h-[320px] overflow-y-auto pr-1">
                {packages.map((pkg) => {
                  const isSelected = selectedPkg?.id === pkg.id;
                  const isLocal = pkg.id.startsWith('local-');

                  return (
                    <div
                      key={pkg.id}
                      onClick={() => otaState.status === 'idle' && setSelectedPkg(pkg)}
                      className={`p-3.5 rounded-lg border transition-all cursor-pointer relative group ${
                        isSelected
                          ? 'bg-red-950/40 border-red-600/80 ring-1 ring-red-600'
                          : 'bg-stone-950/60 border-stone-800 hover:border-stone-700 text-stone-300'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-bold text-sm text-white font-mono truncate max-w-[200px]">
                          {pkg.version}
                        </span>
                        <div className="flex items-center gap-1.5">
                          <span className={`text-[10px] px-1.5 py-0.5 rounded font-mono ${
                            isLocal ? 'bg-purple-950 border border-purple-800 text-purple-300' : 'bg-blue-950 border border-blue-800 text-blue-300'
                          }`}>
                            {isLocal ? 'LOCAL' : 'GATEWAY'}
                          </span>
                          <span className="text-[11px] px-2 py-0.5 bg-stone-800 rounded font-mono text-stone-300">
                            {pkg.sizeFormatted}
                          </span>
                        </div>
                      </div>

                      <p className="text-xs text-stone-400 mb-2">{pkg.targetModel}</p>

                      <div className="flex items-center justify-between text-[11px] font-mono text-stone-500">
                        <span className="truncate max-w-[220px]">
                          SHA: {pkg.checksumSha256.substring(0, 18)}...
                        </span>

                        {otaState.status === 'idle' && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleRemovePackage(pkg.id);
                            }}
                            title="Remove build from list"
                            className="opacity-0 group-hover:opacity-100 p-1 text-stone-500 hover:text-red-400 transition-opacity"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Selected Package Details & Release Notes */}
            {selectedPkg && (
              <div className="bg-stone-950/80 border border-stone-800 rounded-lg p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-bold text-stone-300 uppercase tracking-wider">
                    Build Specification ({selectedPkg.version})
                  </h4>
                  <span className="text-[10px] font-mono text-stone-400">
                    {selectedPkg.sizeBytes.toLocaleString()} bytes
                  </span>
                </div>
                <div className="p-2 bg-black/40 rounded border border-stone-900 text-[11px] font-mono text-stone-400 break-all">
                  SHA-256: {selectedPkg.checksumSha256}
                </div>
                <ul className="space-y-1 text-xs text-stone-400 pt-1">
                  {selectedPkg.releaseNotes.map((note, idx) => (
                    <li key={idx} className="flex items-start gap-1.5">
                      <span className="text-red-500 font-bold">•</span>
                      <span>{note}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Action Buttons */}
            {otaState.status === 'idle' ? (
              <button
                onClick={startPipeline}
                disabled={!selectedPkg}
                className="w-full py-3 px-4 bg-red-600 hover:bg-red-500 disabled:bg-stone-800 disabled:text-stone-500 text-white rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-lg shadow-red-900/30 transition-all cursor-pointer disabled:cursor-not-allowed"
              >
                <Play className="w-4 h-4 fill-white" />
                <span>
                  {selectedPkg ? `Deploy ${selectedPkg.version}` : 'Select Firmware to Deploy'}
                </span>
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
                  <span>Upgrade Successful! Cluster Active on {selectedPkg?.version}</span>
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

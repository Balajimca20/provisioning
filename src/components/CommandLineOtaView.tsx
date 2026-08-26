import React, { useState, useRef, useEffect } from 'react';
import { CommandLineOtaService, OTALogLine } from '../lib/commandLineOtaPipeline';
import { RealtimeAdbClient } from '../lib/realtimeAdbClient';
import { Upload, AlertCircle, RefreshCw, CheckCircle2 } from 'lucide-react';

interface CommandLineOtaViewProps {
  onQuickReboot?: () => void;
}

export const CommandLineOtaView: React.FC<CommandLineOtaViewProps> = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedFileName, setSelectedFileName] = useState<string | null>(null);
  const [selectedFileSizeDescription, setSelectedFileSizeDescription] = useState<string | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);

  const [logLines, setLogLines] = useState<OTALogLine[]>([]);
  const [progress, setProgress] = useState<number>(0);
  const [statusText, setStatusText] = useState<string>('WAITING FOR DEVICE & ZIP PACKAGE…');
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [rebootConsentRequested, setRebootConsentRequested] = useState<boolean>(false);
  const [resultAlertMessage, setResultAlertMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const terminalBottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll terminal log to bottom on each new line
  useEffect(() => {
    terminalBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logLines]);

  const formatFileSize = (bytes: number): string => {
    if (bytes >= 1024 * 1024 * 1024) return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return bytes + ' bytes';
  };

  const handleFilePicked = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.name.endsWith('.zip')) {
      setFileError('The selected file must be a valid OTA package archive (.zip)');
      return;
    }

    setSelectedFile(file);
    setSelectedFileName(file.name);
    setSelectedFileSizeDescription(formatFileSize(file.size));
    setFileError(null);
  };

  const startPipeline = async () => {
    if (!selectedFile || isRunning) return;

    setIsRunning(true);
    setLogLines([]);
    setProgress(0);
    setResultAlertMessage(null);
    setStatusText('🔓 GAINING ROOT ACCESS…');

    const service = CommandLineOtaService.getInstance();
    const generator = service.runPipeline(selectedFile, (p, text) => {
      setProgress(p);
      setStatusText(text);
    });

    let successSignature = false;
    let failureError: string | null = null;

    for await (const packet of generator) {
      setLogLines((prev) => [...prev, packet.log]);
      if (packet.successSignature) {
        successSignature = true;
      }
      if (packet.error) {
        failureError = packet.error;
      }
    }

    setIsRunning(false);

    if (successSignature) {
      setRebootConsentRequested(true);
    } else {
      setStatusText('OTA FAILED');
      setResultAlertMessage(failureError || 'OTA did not report success — review the log for details.');
    }
  };

  const respondToRebootConsent = async (accepted: boolean) => {
    setRebootConsentRequested(false);
    const service = CommandLineOtaService.getInstance();

    if (accepted) {
      setLogLines((prev) => [...prev, service.createLog('🔄 Rebooting device…')]);
      setStatusText('🔄 REBOOTING DEVICE…');
      try {
        await RealtimeAdbClient.getInstance().executeShell('reboot');
      } catch {
        // Continue
      }
      setProgress(1.0);
      setStatusText('OTA COMPLETE');
      setResultAlertMessage('OTA pipeline completed successfully. Device is rebooting.');
    } else {
      setProgress(1.0);
      setStatusText('OTA COMPLETE');
      setResultAlertMessage('OTA pipeline flashed successfully. Reboot skipped.');
    }
  };

  const canStartPipeline = selectedFile !== null && !isRunning;

  const buttonTitle = isRunning
    ? 'OTA UPGRADE RUNNING…'
    : selectedFile === null
    ? 'SELECT UPDATE.ZIP TO RUN OTA'
    : 'START COMMAND LINE OTA UPGRADE';

  return (
    <div className="flex flex-col h-[calc(100vh-140px)] min-h-[580px] bg-[#0F1115] text-white p-4 rounded-xl border border-stone-800 space-y-3">
      {/* Hidden File Picker */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".zip"
        className="hidden"
        onChange={handleFilePicked}
      />

      {/* MARK: - File Picker Section */}
      <div className="flex flex-col space-y-2 shrink-0">
        <div className="flex items-center space-x-3">
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={isRunning}
            className={`px-4 py-2 text-xs font-semibold rounded-lg border transition-colors cursor-pointer flex items-center space-x-2 ${
              isRunning
                ? 'border-stone-700 text-stone-600 cursor-not-allowed bg-stone-900'
                : 'border-[#00D2B4] text-white hover:bg-[#00D2B4]/10 bg-transparent'
            }`}
          >
            <Upload className="w-3.5 h-3.5 text-[#00D2B4]" />
            <span>Select OTA Package (.zip)</span>
          </button>

          {selectedFileName && (
            <div className="flex flex-col min-w-0">
              <span className="font-mono text-xs font-semibold text-white truncate max-w-md">
                {selectedFileName}
              </span>
              {selectedFileSizeDescription && (
                <span className="text-[11px] text-[#8E929B] font-mono">
                  {selectedFileSizeDescription}
                </span>
              )}
            </div>
          )}
        </div>

        {/* Status Text with Dynamic Colors */}
        <div className="text-xs font-mono font-semibold text-[#00D2B4] tracking-wide">
          {statusText}
        </div>
      </div>

      {/* MARK: - Monospaced Pure Black Terminal Screen */}
      <div className="flex-1 bg-black border border-stone-800 rounded-lg p-3 overflow-y-auto font-mono text-[11px] leading-relaxed select-text">
        {logLines.length === 0 ? (
          <div className="text-stone-600 italic py-2">
            // Command line OTA log terminal initialized. Select update.zip package above to start deployment.
          </div>
        ) : (
          logLines.map((line) => (
            <div key={line.id} className="text-[#00FF40] whitespace-pre-wrap break-all py-0.5">
              {line.text}
            </div>
          ))
        )}
        <div ref={terminalBottomRef} />
      </div>

      {/* MARK: - Progress View & Percentage Indicator */}
      <div className="flex items-center space-x-3 shrink-0">
        <div className="flex-1 bg-stone-800 rounded-full h-2 overflow-hidden">
          <div
            className="bg-[#00D2B4] h-full transition-all duration-300 rounded-full"
            style={{ width: `${Math.min(100, Math.max(0, Math.round(progress * 100)))}%` }}
          />
        </div>
        <span className="font-mono text-xs font-semibold text-[#00D2B4] min-w-[48px] text-right">
          ({Math.round(progress * 100)}%)
        </span>
      </div>

      {/* MARK: - Primary Action Button */}
      <button
        onClick={startPipeline}
        disabled={!canStartPipeline}
        className={`w-full py-3 px-4 rounded-lg font-bold text-xs tracking-wider transition-all duration-200 flex items-center justify-center space-x-2 shrink-0 ${
          canStartPipeline
            ? 'bg-[#00D2B4] hover:bg-[#00bda2] text-black cursor-pointer shadow-lg shadow-[#00D2B4]/20'
            : isRunning
            ? 'bg-[#00D2B4]/30 text-stone-400 cursor-not-allowed'
            : 'bg-stone-800 text-stone-500 cursor-not-allowed'
        }`}
      >
        {isRunning && <RefreshCw className="w-4 h-4 animate-spin text-[#00D2B4]" />}
        <span>{buttonTitle}</span>
      </button>

      {/* MARK: - Alert Modals matching Swift code */}
      {rebootConsentRequested && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fade-in">
          <div className="bg-[#161920] border border-stone-700 rounded-xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center space-x-3 text-white">
              <AlertCircle className="w-6 h-6 text-[#00D2B4]" />
              <h3 className="text-base font-bold">Reboot Required</h3>
            </div>
            <p className="text-sm text-stone-300">
              Command line OTA update done, need a reboot to use the new software. Reboot now?
            </p>
            <div className="flex justify-end space-x-3 pt-2">
              <button
                onClick={() => respondToRebootConsent(false)}
                className="px-4 py-2 rounded-lg text-xs font-semibold text-stone-400 hover:text-white transition-colors cursor-pointer"
              >
                No
              </button>
              <button
                onClick={() => respondToRebootConsent(true)}
                className="px-5 py-2 rounded-lg text-xs font-bold bg-[#00D2B4] hover:bg-[#00bda2] text-black transition-colors cursor-pointer"
              >
                Yes
              </button>
            </div>
          </div>
        </div>
      )}

      {fileError && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fade-in">
          <div className="bg-[#161920] border border-red-500/50 rounded-xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center space-x-3 text-red-400">
              <AlertCircle className="w-6 h-6" />
              <h3 className="text-base font-bold">File Error</h3>
            </div>
            <p className="text-sm text-stone-300">{fileError}</p>
            <div className="flex justify-end pt-2">
              <button
                onClick={() => setFileError(null)}
                className="px-5 py-2 rounded-lg text-xs font-bold bg-[#00D2B4] text-black cursor-pointer"
              >
                OK
              </button>
            </div>
          </div>
        </div>
      )}

      {resultAlertMessage && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fade-in">
          <div className="bg-[#161920] border border-stone-700 rounded-xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center space-x-3 text-white">
              <CheckCircle2 className="w-6 h-6 text-[#00D2B4]" />
              <h3 className="text-base font-bold">OTA Result</h3>
            </div>
            <p className="text-sm text-stone-300">{resultAlertMessage}</p>
            <div className="flex justify-end pt-2">
              <button
                onClick={() => setResultAlertMessage(null)}
                className="px-5 py-2 rounded-lg text-xs font-bold bg-[#00D2B4] text-black cursor-pointer"
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

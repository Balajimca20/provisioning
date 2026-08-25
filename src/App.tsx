import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { DashboardView } from './components/DashboardView';
import { WifiView } from './components/WifiView';
import { OtaView } from './components/OtaView';
import { SupplierFeedView } from './components/SupplierFeedView';
import { TerminalDrawer } from './components/TerminalDrawer';
import { AdbState, EnvironmentType, TabType, WifiLogRecord, WifiState } from './types';
import { loadWifiChangeLogs } from './lib/wifiStore';
import { RealtimeAdbClient } from './lib/realtimeAdbClient';
import { Power, CheckCircle2, X } from 'lucide-react';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');
  const [env, setEnv] = useState<EnvironmentType>('prod');

  const [wifiState, setWifiState] = useState<WifiState>({
    status: 'connected',
    ssid: 'RE_LXHD_250925',
    password: '••••••••',
    ipAddress: '192.168.1.45',
    gateway: '192.168.1.1',
    signalStrength: -42,
    connectedAt: new Date().toLocaleTimeString(),
  });

  const [adbState, setAdbState] = useState<AdbState>({
    status: 'connected',
    host: '192.168.1.1',
    port: 5555,
    isRoot: true,
    deviceModel: 'TFT Instrument Cluster (Tripper Pro v3.2)',
    serialNumber: 'RE-HIM450-SN-8921094',
    androidVersion: 'Android 12 Automotive (API 32 - RE-OS 4.8.1)',
    connectedAt: new Date().toLocaleTimeString(),
  });

  const [changeLogs, setChangeLogs] = useState<WifiLogRecord[]>(() => loadWifiChangeLogs());
  const [rebootModalOpen, setRebootModalOpen] = useState(false);
  const [isRebooting, setIsRebooting] = useState(false);
  const [rebootFinished, setRebootFinished] = useState(false);

  const handleConnectWifi = async (ssid: string, password?: string) => {
    setWifiState({
      status: 'connecting',
      ssid,
    });

    await new Promise((r) => setTimeout(r, 1200));

    setWifiState({
      status: 'connected',
      ssid,
      password: password || '••••••••',
      ipAddress: '192.168.1.45',
      gateway: '192.168.1.1',
      signalStrength: -45,
      connectedAt: new Date().toLocaleTimeString(),
    });
  };

  const handleDisconnectWifi = () => {
    setWifiState({
      status: 'disconnected',
      ssid: '',
    });
  };

  const handleConnectAdb = async (host: string, port: number) => {
    setAdbState((prev) => ({
      ...prev,
      status: 'connecting',
      host,
      port,
    }));

    const engine = RealtimeAdbClient.getInstance();
    const res = await engine.connectAdb(host, port);

    if (res.success && res.adbState) {
      setAdbState((prev) => ({
        ...prev,
        ...res.adbState,
        status: 'connected',
      }));
    } else {
      setAdbState((prev) => ({
        ...prev,
        status: 'error',
        error: res.error,
      }));
    }
  };

  const handleDisconnectAdb = () => {
    setAdbState((prev) => ({
      ...prev,
      status: 'disconnected',
    }));
  };

  const handleTriggerReboot = async () => {
    setIsRebooting(true);
    const engine = RealtimeAdbClient.getInstance();
    await engine.executeShell('reboot');
    await new Promise((r) => setTimeout(r, 2000));
    setIsRebooting(false);
    setRebootFinished(true);
    setTimeout(() => {
      setRebootFinished(false);
      setRebootModalOpen(false);
    }, 1500);
  };

  return (
    <div className="min-h-screen bg-stone-950 text-stone-100 flex flex-col">
      {/* Top Navbar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        wifiState={wifiState}
        adbState={adbState}
        env={env}
        setEnv={setEnv}
        onQuickReboot={() => setRebootModalOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === 'dashboard' && (
          <DashboardView
            wifiState={wifiState}
            adbState={adbState}
            onConnectWifi={handleConnectWifi}
            onDisconnectWifi={handleDisconnectWifi}
            onConnectAdb={handleConnectAdb}
            onDisconnectAdb={handleDisconnectAdb}
            setActiveTab={setActiveTab}
            onQuickReboot={() => setRebootModalOpen(true)}
          />
        )}

        {activeTab === 'wifi' && (
          <WifiView
            wifiState={wifiState}
            adbState={adbState}
            changeLogs={changeLogs}
            setChangeLogs={setChangeLogs}
            onQuickReboot={() => setRebootModalOpen(true)}
          />
        )}

        {activeTab === 'ota' && (
          <OtaView
            onQuickReboot={() => setRebootModalOpen(true)}
          />
        )}

        {activeTab === 'supplier' && <SupplierFeedView />}

        {activeTab === 'terminal' && <TerminalDrawer adbState={adbState} />}
      </main>

      {/* Footer */}
      <footer className="border-t border-stone-800/80 bg-stone-900/50 py-4 mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs font-mono text-stone-400">
          <div className="flex items-center gap-2">
            <span className="font-bold text-stone-300">FF PROVISIONING</span>
            <span className="text-stone-600">•</span>
            <span>Vehicle Diagnostics & Provisioning</span>
          </div>
          <div className="flex items-center gap-3">
            <span>ADB Bridge: 192.168.1.1:5555</span>
            <span className="text-stone-600">•</span>
            <span>SoftAP: RE_LXHD_250925</span>
          </div>
        </div>
      </footer>

      {/* Reboot Modal */}
      {rebootModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-stone-900 border border-stone-800 rounded-xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-stone-800 pb-3">
              <div className="flex items-center gap-2 text-red-500 font-bold text-base">
                <Power className="w-5 h-5" />
                <span>Reboot Vehicle Cluster</span>
              </div>
              <button
                onClick={() => setRebootModalOpen(false)}
                className="text-stone-400 hover:text-stone-200"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {rebootFinished ? (
              <div className="p-4 bg-emerald-950/80 border border-emerald-700 rounded-lg text-center space-y-2">
                <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto" />
                <h4 className="font-bold text-emerald-300 text-sm">Reboot Command Sent!</h4>
                <p className="text-xs text-stone-300">
                  Vehicle instrument cluster is restarting into normal operating mode.
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                <p className="text-xs text-stone-300 leading-relaxed">
                  Are you sure you want to send the ADB reboot command to{' '}
                  <span className="font-mono text-cyan-400 font-bold">192.168.1.1:5555</span>?
                  This will restart the Android Automotive cluster subsystem and apply pending SoftAP
                  or OTA updates.
                </p>

                <div className="flex gap-3">
                  <button
                    onClick={() => setRebootModalOpen(false)}
                    className="flex-1 py-2.5 px-4 bg-stone-800 hover:bg-stone-700 text-stone-300 text-xs font-bold rounded-lg transition-colors cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleTriggerReboot}
                    disabled={isRebooting}
                    className="flex-1 py-2.5 px-4 bg-red-600 hover:bg-red-500 text-white text-xs font-bold rounded-lg transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg shadow-red-900/40"
                  >
                    <Power className="w-4 h-4" />
                    <span>{isRebooting ? 'Rebooting...' : 'Confirm Reboot'}</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

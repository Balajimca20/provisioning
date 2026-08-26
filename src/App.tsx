import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { DashboardView } from './components/DashboardView';
import { CommandLineOtaView } from './components/CommandLineOtaView';
import { OtaView } from './components/OtaView';
import { WifiView } from './components/WifiView';
import { TerminalView } from './components/TerminalView';
import { SupplierFeedView } from './components/SupplierFeedView';
import type { TabType, DeviceConnectionState } from './types';

export function App() {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');
  const [deviceState, setDeviceState] = useState<DeviceConnectionState>({
    isWifiConnected: true,
    connectedSsid: 'RE_Hunter_350_AP',
    isAdbConnected: true,
    adbHost: '192.168.43.1',
    adbPort: '5555',
    batteryVoltage: '13.4 V',
    ecuStatus: 'SYNCED (500k)',
    storageSpace: '11.8 GB free',
    activeSlot: 'A',
    buildVariant: 'DEV',
    serialNumber: 'RE-HNT350-IN-9812401'
  });

  const handleConnectWifi = (ssid: string) => {
    setDeviceState(prev => ({
      ...prev,
      isWifiConnected: true,
      connectedSsid: ssid
    }));
  };

  const handleConnectAdb = (host: string, port: string) => {
    setDeviceState(prev => ({
      ...prev,
      isAdbConnected: true,
      adbHost: host,
      adbPort: port
    }));
  };

  const handleDisconnect = () => {
    setDeviceState(prev => ({
      ...prev,
      isWifiConnected: false,
      connectedSsid: '',
      isAdbConnected: false
    }));
  };

  const handleUpdateVariant = (variant: 'DEV' | 'UAT' | 'PROD') => {
    setDeviceState(prev => ({
      ...prev,
      buildVariant: variant
    }));
  };

  return (
    <div className="min-h-screen bg-[#0F1115] text-slate-100 flex flex-col">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        deviceState={deviceState}
        onDisconnect={handleDisconnect}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6">
        {activeTab === 'dashboard' && (
          <DashboardView
            deviceState={deviceState}
            setActiveTab={setActiveTab}
            onUpdateVariant={handleUpdateVariant}
          />
        )}

        {activeTab === 'cmd_ota' && (
          <CommandLineOtaView
            deviceState={deviceState}
            onNavigateToWifi={() => setActiveTab('wifi')}
            onNavigateToTerminal={() => setActiveTab('terminal')}
          />
        )}

        {activeTab === 'ota' && (
          <OtaView deviceState={deviceState} />
        )}

        {activeTab === 'wifi' && (
          <WifiView
            deviceState={deviceState}
            onConnectWifi={handleConnectWifi}
            onConnectAdb={handleConnectAdb}
            onDisconnect={handleDisconnect}
          />
        )}

        {activeTab === 'terminal' && (
          <TerminalView deviceState={deviceState} />
        )}

        {activeTab === 'supplier' && (
          <SupplierFeedView deviceState={deviceState} />
        )}
      </main>

      <footer className="border-t border-white/5 py-4 px-6 text-center text-xs text-slate-500 font-mono">
        FF Provisioning Suite • Royal Enfield Connected Ecosystem • Android Module (:app) Ready
      </footer>
    </div>
  );
}

export default App;

import React, { useState, useEffect } from 'react';
import {
  Cpu,
  Search,
  XCircle,
  RefreshCw,
  FileCode,
  Zap,
  Battery,
  Globe,
  Activity,
  Clock,
  Code2,
} from 'lucide-react';
import { DeviceTelemetry, LiveGraphQLMeta, SupplierBannerState } from '../types';
import {
  RealtimeGraphQLClient,
  DEFAULT_GRAPHQL_ENDPOINT,
  VEHICLE_DIAGNOSTIC_QUERY,
} from '../lib/realtimeGraphQLClient';

export const SupplierFeedView: React.FC = () => {
  const [endpointUrl, setEndpointUrl] = useState(DEFAULT_GRAPHQL_ENDPOINT);
  const [apiKey, setApiKey] = useState('');
  const [serialQuery, setSerialQuery] = useState('RE-HIM450-SN-8921094');
  const [bannerState, setBannerState] = useState<SupplierBannerState>('idle');
  const [deviceData, setDeviceData] = useState<DeviceTelemetry | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [rawResponseJson, setRawResponseJson] = useState<string>('');
  const [queryMeta, setQueryMeta] = useState<LiveGraphQLMeta | null>(null);
  const [activeTab, setActiveTab] = useState<'telemetry' | 'raw_json' | 'schema'>('telemetry');
  const [endpointModalOpen, setEndpointModalOpen] = useState(false);

  const client = new RealtimeGraphQLClient(endpointUrl, apiKey || undefined);

  const executeRealtimeQuery = async (sn: string) => {
    const cleanSn = sn.trim();
    if (!cleanSn) return;

    setBannerState('loading');
    setErrorMessage('');

    const res = await client.fetchRealtimeDevice(cleanSn);
    setQueryMeta(res.meta);
    setRawResponseJson(res.rawJson || '');

    if (res.error) {
      setBannerState('error');
      setErrorMessage(res.error);
      setDeviceData(null);
    } else if (res.data?.getDevice) {
      setBannerState('loaded');
      setDeviceData(res.data.getDevice);
    } else {
      setBannerState('not_found');
      setDeviceData(null);
      setErrorMessage(`No active diagnostic telemetry record received for serial: ${cleanSn}`);
    }
  };

  useEffect(() => {
    // Initial check on mount
    executeRealtimeQuery(serialQuery);
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    executeRealtimeQuery(serialQuery);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Cpu className="w-5 h-5 text-red-500" />
            <h2 className="text-lg font-bold text-white">Supplier Diagnostic Feed</h2>
            <span className="px-2 py-0.5 bg-emerald-950/80 border border-emerald-700/60 text-emerald-400 font-mono text-[11px] font-bold rounded">
              REAL-TIME GATEWAY
            </span>
          </div>
          <p className="text-xs text-stone-400 mt-1 font-mono">
            Direct GraphQL Client • Live Telemetry, Diagnostic Fault Codes & Component Registry
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setEndpointModalOpen(!endpointModalOpen)}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-stone-950 border border-stone-800 hover:border-stone-700 rounded-lg text-xs font-mono text-stone-300 transition-colors cursor-pointer"
          >
            <Globe className="w-3.5 h-3.5 text-cyan-400" />
            <span>Endpoint: {endpointUrl.replace('http://', '').replace('/graphql', '')}</span>
          </button>

          <button
            onClick={() => setActiveTab(activeTab === 'schema' ? 'telemetry' : 'schema')}
            className={`flex items-center gap-1.5 px-3 py-1.5 border rounded-lg text-xs font-mono transition-colors cursor-pointer ${
              activeTab === 'schema'
                ? 'bg-red-950 border-red-700 text-red-300'
                : 'bg-stone-950 border-stone-800 text-stone-300 hover:border-stone-700'
            }`}
          >
            <FileCode className="w-3.5 h-3.5 text-cyan-400" />
            <span>Schema</span>
          </button>
        </div>
      </div>

      {/* Endpoint Configuration Modal / Card */}
      {endpointModalOpen && (
        <div className="bg-stone-950 border border-stone-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-stone-800 pb-2">
            <h3 className="text-xs font-bold uppercase tracking-wider text-stone-200 flex items-center gap-2 font-mono">
              <Globe className="w-4 h-4 text-cyan-400" />
              <span>Configure Live GraphQL Endpoint</span>
            </h3>
            <button
              onClick={() => setEndpointModalOpen(false)}
              className="text-stone-500 hover:text-stone-300 text-xs font-mono"
            >
              [Close]
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono">
            <div>
              <label className="block text-stone-400 mb-1">Target GraphQL Gateway URL</label>
              <input
                type="text"
                value={endpointUrl}
                onChange={(e) => setEndpointUrl(e.target.value)}
                placeholder="http://192.168.1.1:8080/graphql"
                className="w-full bg-stone-900 border border-stone-700 px-3 py-2 rounded text-white focus:outline-none focus:border-red-500"
              />
            </div>
            <div>
              <label className="block text-stone-400 mb-1">Authorization Bearer Token (Optional)</label>
              <input
                type="password"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder="Bearer JWT or API Key"
                className="w-full bg-stone-900 border border-stone-700 px-3 py-2 rounded text-white focus:outline-none focus:border-red-500"
              />
            </div>
          </div>

          <div className="flex items-center gap-2 text-xs font-mono">
            <span className="text-stone-500">Quick Endpoints:</span>
            <button
              onClick={() => setEndpointUrl('http://192.168.1.1:8080/graphql')}
              className="px-2 py-1 bg-stone-900 hover:bg-stone-800 border border-stone-800 rounded text-stone-300"
            >
              Vehicle Cluster (192.168.1.1:8080)
            </button>
            <button
              onClick={() => setEndpointUrl('https://api.ffmechanic.royalenfield.com/graphql')}
              className="px-2 py-1 bg-stone-900 hover:bg-stone-800 border border-stone-800 rounded text-stone-300"
            >
              Cloud Supplier Hub
            </button>
          </div>
        </div>
      )}

      {/* Query Bar */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 space-y-4">
        <form onSubmit={handleSearchSubmit} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-stone-500 absolute left-3.5 top-3" />
            <input
              type="text"
              value={serialQuery}
              onChange={(e) => setSerialQuery(e.target.value.toUpperCase())}
              placeholder="Query vehicle by Serial Number or VIN (e.g. RE-HIM450-SN-8921094)..."
              className="w-full bg-stone-950 border border-stone-700 pl-10 pr-4 py-2.5 rounded-lg text-sm font-mono text-white placeholder-stone-600 focus:outline-none focus:border-red-500"
            />
          </div>
          <button
            type="submit"
            disabled={bannerState === 'loading'}
            className="px-5 py-2.5 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm font-bold flex items-center gap-2 shadow-lg shadow-red-900/30 transition-all cursor-pointer"
          >
            {bannerState === 'loading' ? (
              <RefreshCw className="w-4 h-4 animate-spin" />
            ) : (
              <Search className="w-4 h-4" />
            )}
            <span>Execute Live Query</span>
          </button>
        </form>

        {/* Live Network Metrics Bar */}
        {queryMeta && (
          <div className="flex items-center justify-between flex-wrap gap-2 text-xs font-mono bg-stone-950/70 border border-stone-800/80 px-3.5 py-2 rounded-lg text-stone-400">
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1.5 text-stone-300">
                <Activity className="w-3.5 h-3.5 text-cyan-400" />
                <span>RTT Latency:</span>
                <span className="text-cyan-400 font-bold">{queryMeta.latencyMs} ms</span>
              </span>
              <span className="text-stone-600">•</span>
              <span className="flex items-center gap-1.5 text-stone-300">
                <span>HTTP Status:</span>
                <span
                  className={`font-bold ${
                    queryMeta.httpStatus === 200
                      ? 'text-emerald-400'
                      : queryMeta.httpStatus === 0
                      ? 'text-amber-400'
                      : 'text-red-400'
                  }`}
                >
                  {queryMeta.httpStatus === 0 ? 'SOCKET PENDING' : queryMeta.httpStatus}
                </span>
              </span>
            </div>

            <div className="flex items-center gap-3 text-stone-500">
              <span className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                <span>{new Date(queryMeta.timestamp).toLocaleTimeString()}</span>
              </span>
              <span>Payload: {queryMeta.querySize} bytes</span>
            </div>
          </div>
        )}
      </div>

      {/* View Tabs */}
      <div className="flex items-center gap-2 border-b border-stone-800 pb-2 text-xs font-mono">
        <button
          onClick={() => setActiveTab('telemetry')}
          className={`px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer ${
            activeTab === 'telemetry'
              ? 'bg-stone-800 text-white font-bold'
              : 'text-stone-400 hover:text-stone-200'
          }`}
        >
          <Cpu className="w-3.5 h-3.5 text-red-400" />
          <span>Telemetry Dashboard</span>
        </button>
        <button
          onClick={() => setActiveTab('raw_json')}
          className={`px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer ${
            activeTab === 'raw_json'
              ? 'bg-stone-800 text-white font-bold'
              : 'text-stone-400 hover:text-stone-200'
          }`}
        >
          <Code2 className="w-3.5 h-3.5 text-cyan-400" />
          <span>Raw GraphQL Response JSON</span>
        </button>
        <button
          onClick={() => setActiveTab('schema')}
          className={`px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer ${
            activeTab === 'schema'
              ? 'bg-stone-800 text-white font-bold'
              : 'text-stone-400 hover:text-stone-200'
          }`}
        >
          <FileCode className="w-3.5 h-3.5 text-amber-400" />
          <span>GraphQL Query AST</span>
        </button>
      </div>

      {/* Schema Tab */}
      {activeTab === 'schema' && (
        <div className="bg-stone-950 border border-stone-800 rounded-xl p-4 space-y-2 font-mono text-xs">
          <div className="flex items-center justify-between text-stone-400 border-b border-stone-900 pb-2">
            <span>GraphQL Endpoint: POST {endpointUrl}</span>
            <span className="text-cyan-400">Variable: {`{ "serial": "${serialQuery}" }`}</span>
          </div>
          <pre className="text-emerald-400 text-[11px] leading-relaxed overflow-x-auto bg-black/60 p-3 rounded">
            {VEHICLE_DIAGNOSTIC_QUERY}
          </pre>
        </div>
      )}

      {/* Raw JSON Tab */}
      {activeTab === 'raw_json' && (
        <div className="bg-stone-950 border border-stone-800 rounded-xl p-4 space-y-2 font-mono text-xs">
          <div className="flex items-center justify-between text-stone-400 border-b border-stone-900 pb-2">
            <span>Real-time Payload Dump</span>
            <span className="text-stone-500">Length: {rawResponseJson.length} chars</span>
          </div>
          <pre className="text-cyan-300 text-[11px] leading-relaxed overflow-x-auto bg-black/80 p-4 rounded max-h-[500px] overflow-y-auto">
            {rawResponseJson || '// No active response payload'}
          </pre>
        </div>
      )}

      {/* Main Telemetry Tab */}
      {activeTab === 'telemetry' && (
        <>
          {/* Loading */}
          {bannerState === 'loading' && (
            <div className="bg-stone-900 border border-stone-800 rounded-xl p-12 text-center space-y-3">
              <RefreshCw className="w-8 h-8 text-red-500 animate-spin mx-auto" />
              <h3 className="font-bold text-stone-200 text-base">
                Executing Live GraphQL Query...
              </h3>
              <p className="text-xs text-stone-500 font-mono">
                Connecting to {endpointUrl} for real-time telemetry verification
              </p>
            </div>
          )}

          {/* Error / Offline Banner */}
          {bannerState === 'error' && (
            <div className="bg-red-950/40 border border-red-800 rounded-xl p-6 flex items-start gap-4">
              <XCircle className="w-6 h-6 text-red-400 shrink-0 mt-0.5" />
              <div className="space-y-1">
                <h3 className="font-bold text-red-200 text-sm">Real-time Query Error</h3>
                <p className="text-xs text-red-300 font-mono">{errorMessage}</p>
                <div className="pt-2 text-xs text-stone-400">
                  <span>Troubleshooting: </span>
                  <span className="text-stone-300">
                    Verify that the vehicle cluster SoftAP is connected and the GraphQL daemon is
                    active on port 8080.
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Loaded Device Profile */}
          {bannerState === 'loaded' && deviceData && (
            <div className="space-y-6">
              {/* Identity Header */}
              <div className="bg-gradient-to-br from-stone-900 via-stone-900 to-stone-950 border border-stone-800 rounded-xl p-6 shadow-xl relative overflow-hidden">
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                  <div className="space-y-2">
                    <div className="flex items-center gap-2.5 flex-wrap">
                      <h3 className="text-xl font-extrabold text-white tracking-wide">
                        {deviceData.model}
                      </h3>
                      <span className="px-2.5 py-0.5 bg-emerald-950 text-emerald-400 border border-emerald-800 rounded font-mono text-xs font-bold">
                        {deviceData.warrantyStatus} WARRANTY
                      </span>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-x-6 gap-y-1 font-mono text-xs text-stone-400">
                      <div>
                        <span className="text-stone-600">VIN: </span>
                        <span className="text-stone-200 font-semibold">{deviceData.vin}</span>
                      </div>
                      <div>
                        <span className="text-stone-600">Serial: </span>
                        <span className="text-cyan-400 font-semibold">
                          {deviceData.serialNumber}
                        </span>
                      </div>
                      <div>
                        <span className="text-stone-600">Firmware: </span>
                        <span className="text-stone-300">{deviceData.firmwareVersion}</span>
                      </div>
                      <div>
                        <span className="text-stone-600">Supplier: </span>
                        <span className="text-stone-300">{deviceData.supplierName}</span>
                      </div>
                      <div>
                        <span className="text-stone-600">Manufacture: </span>
                        <span className="text-stone-300">{deviceData.manufactureDate}</span>
                      </div>
                      <div>
                        <span className="text-stone-600">Odometer: </span>
                        <span className="text-stone-200">{deviceData.odometerKm} km</span>
                      </div>
                    </div>
                  </div>

                  {/* Badges */}
                  <div className="flex flex-wrap lg:flex-col gap-3">
                    <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950/80 border border-stone-800 rounded-lg text-xs font-mono">
                      <Battery className="w-4 h-4 text-emerald-400" />
                      <span className="text-stone-400">Battery:</span>
                      <span className="text-white font-bold">
                        {deviceData.batteryVoltage}V ({deviceData.batterySoC}%)
                      </span>
                    </div>
                    <div className="flex items-center gap-2 px-3 py-1.5 bg-stone-950/80 border border-stone-800 rounded-lg text-xs font-mono">
                      <Zap className="w-4 h-4 text-amber-400" />
                      <span className="text-stone-400">ECU Link:</span>
                      <span className="text-emerald-400 font-bold">{deviceData.ecuStatus}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Categorized Telemetry Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {deviceData.fields?.map((cat, idx) => (
                  <div
                    key={idx}
                    className="bg-stone-900 border border-stone-800 rounded-xl p-5 space-y-3"
                  >
                    <h4 className="text-xs font-bold text-stone-300 uppercase tracking-wider border-b border-stone-800 pb-2 flex items-center justify-between">
                      <span>{cat.category}</span>
                      <span className="text-[10px] text-stone-500 font-mono">
                        {cat.items.length} Parameters
                      </span>
                    </h4>

                    <div className="space-y-2.5">
                      {cat.items.map((item) => (
                        <div
                          key={item.key}
                          className="flex items-start justify-between p-2.5 bg-stone-950/60 border border-stone-800/80 rounded-lg text-xs font-mono"
                        >
                          <div className="space-y-0.5 max-w-[50%]">
                            <span className="text-stone-400 block">{item.label}</span>
                            <span className="text-[10px] text-stone-600">{item.key}</span>
                          </div>
                          <div className="text-right">
                            <span
                              className={`font-semibold ${
                                item.status === 'warn'
                                  ? 'text-amber-400'
                                  : item.status === 'alert'
                                  ? 'text-red-400 font-bold'
                                  : 'text-stone-200'
                              }`}
                            >
                              {item.value}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

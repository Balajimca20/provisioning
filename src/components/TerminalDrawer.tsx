import React, { useState, useRef, useEffect } from 'react';
import { Terminal, Send, Trash2 } from 'lucide-react';
import { AdbState } from '../types';
import { RealtimeAdbClient } from '../lib/realtimeAdbClient';

interface TerminalDrawerProps {
  adbState: AdbState;
}

interface CommandEntry {
  id: string;
  command: string;
  output: string;
  timestamp: string;
  isError?: boolean;
}

let terminalCounter = 0;

export const TerminalDrawer: React.FC<TerminalDrawerProps> = ({ adbState }) => {
  const [commandInput, setCommandInput] = useState('');
  const [history, setHistory] = useState<CommandEntry[]>([
    {
      id: 'init-1',
      command: 'su 0 id',
      output: 'uid=0(root) gid=0(root) groups=0(root) context=u:r:su:s0',
      timestamp: new Date().toLocaleTimeString(),
    },
    {
      id: 'init-2',
      command: 'getprop ro.product.model',
      output: 'RE-TFT-CLUSTER-PRO-2026',
      timestamp: new Date().toLocaleTimeString(),
    },
  ]);
  const [isExecuting, setIsExecuting] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [history]);

  const executeCommand = async (cmd: string) => {
    const trimmed = cmd.trim();
    if (!trimmed) return;

    setIsExecuting(true);
    const engine = RealtimeAdbClient.getInstance();
    const result = await engine.executeShell(trimmed);

    setHistory((prev) => [
      ...prev,
      {
        id: `cmd-${Date.now()}-${++terminalCounter}-${Math.random().toString(36).substring(2, 7)}`,
        command: trimmed,
        output: result,
        timestamp: new Date().toLocaleTimeString(),
      },
    ]);

    setCommandInput('');
    setIsExecuting(false);
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    executeCommand(commandInput);
  };

  const quickCommands = [
    'su 0 id',
    'getprop ro.product.model',
    'dumpsys battery',
    'cat /data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml',
    'ls -la /data/misc/apexdata/com.android.wifi',
    'df -h /data',
    'reboot',
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-stone-900 border border-stone-800 rounded-xl p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Terminal className="w-5 h-5 text-red-500" />
            <h2 className="text-lg font-bold text-white">ADB Shell Terminal</h2>
          </div>
          <p className="text-xs text-stone-400 mt-1 font-mono">
            Direct ADB TCP daemon interaction • Target: {adbState.host}:{adbState.port}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setHistory([])}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-stone-950 border border-stone-800 hover:border-stone-700 rounded-lg text-xs font-mono text-stone-400 hover:text-stone-200 transition-colors"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Clear Console</span>
          </button>
        </div>
      </div>

      {/* Quick Command Toolbar */}
      <div className="flex items-center gap-2 flex-wrap text-xs">
        <span className="text-stone-500 font-mono">Common ADB Commands:</span>
        {quickCommands.map((cmd) => (
          <button
            key={cmd}
            onClick={() => executeCommand(cmd)}
            disabled={isExecuting}
            className="px-2.5 py-1 bg-stone-900 hover:bg-stone-800 border border-stone-800 hover:border-stone-700 text-stone-300 rounded font-mono text-[11px] transition-colors"
          >
            {cmd}
          </button>
        ))}
      </div>

      {/* Terminal Window */}
      <div className="bg-black border border-stone-800 rounded-xl overflow-hidden shadow-2xl flex flex-col h-[520px]">
        {/* Terminal Titlebar */}
        <div className="bg-stone-900/90 px-4 py-2.5 border-b border-stone-800 flex items-center justify-between font-mono text-xs text-stone-400">
          <div className="flex items-center gap-2">
            <div className="flex gap-1.5">
              <div className="w-2.5 h-2.5 rounded-full bg-red-500/80" />
              <div className="w-2.5 h-2.5 rounded-full bg-amber-500/80" />
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/80" />
            </div>
            <span className="text-stone-300 font-bold ml-2">re_vehicle_shell</span>
          </div>
          <span className="text-emerald-400 font-bold">root@re_cluster #</span>
        </div>

        {/* Scrollable Command Logs */}
        <div ref={scrollRef} className="flex-1 p-4 font-mono text-xs overflow-y-auto space-y-4">
          <div className="text-stone-500">
            Royal Enfield Android Automotive Diagnostic Shell (Linux 5.10.104-android12-9-g3918)
            <br />
            Type commands or select from presets above.
          </div>

          {history.map((entry) => (
            <div key={entry.id} className="space-y-1">
              <div className="flex items-center gap-2 text-cyan-400 font-semibold">
                <span className="text-stone-500 select-none">[{entry.timestamp}]</span>
                <span className="text-red-500 select-none">re_cluster:/ #</span>
                <span>{entry.command}</span>
              </div>
              <pre className="text-stone-300 text-xs pl-4 border-l border-stone-800 whitespace-pre-wrap leading-relaxed">
                {entry.output}
              </pre>
            </div>
          ))}

          {isExecuting && (
            <div className="flex items-center gap-2 text-stone-400 animate-pulse">
              <span className="text-red-500 select-none">re_cluster:/ #</span>
              <span>executing...</span>
            </div>
          )}
        </div>

        {/* Command Input Bar */}
        <form onSubmit={handleFormSubmit} className="p-3 bg-stone-900 border-t border-stone-800 flex gap-2">
          <div className="flex items-center gap-2 flex-1 bg-black border border-stone-700 px-3 py-2 rounded-lg font-mono text-xs">
            <span className="text-red-500 font-bold select-none">#</span>
            <input
              type="text"
              value={commandInput}
              onChange={(e) => setCommandInput(e.target.value)}
              placeholder="Enter ADB command (e.g. su 0 id, dumpsys wifi, reboot)..."
              className="w-full bg-transparent text-white focus:outline-none placeholder-stone-600"
            />
          </div>
          <button
            type="submit"
            disabled={isExecuting || !commandInput.trim()}
            className={`px-4 py-2 rounded-lg text-xs font-bold font-mono flex items-center gap-1.5 transition-colors cursor-pointer ${
              commandInput.trim() && !isExecuting
                ? 'bg-red-600 hover:bg-red-500 text-white'
                : 'bg-stone-800 text-stone-500 cursor-not-allowed'
            }`}
          >
            <Send className="w-3.5 h-3.5" />
            <span>Send</span>
          </button>
        </form>
      </div>
    </div>
  );
};

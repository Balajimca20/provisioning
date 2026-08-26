import React, { useState, useRef, useEffect } from 'react';
import { Terminal as TerminalIcon, Play, Trash2, Shield, Info } from 'lucide-react';
import type { DeviceConnectionState, TerminalLogLine } from '../types';

interface TerminalViewProps {
  deviceState: DeviceConnectionState;
}

export const TerminalView: React.FC<TerminalViewProps> = ({ deviceState }) => {
  const [command, setCommand] = useState('');
  const [history, setHistory] = useState<TerminalLogLine[]>([
    {
      id: 't-1',
      timestamp: new Date().toLocaleTimeString(),
      text: 'Connected to Royal Enfield Telematics Control Unit (adbd daemon uid=0)',
      type: 'success'
    },
    {
      id: 't-2',
      timestamp: new Date().toLocaleTimeString(),
      text: 'Type low-level shell commands (e.g., getprop, df -h, logcat -d, get_active_slot, ps)',
      type: 'info'
    }
  ]);

  const outputRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (outputRef.current) {
      outputRef.current.scrollTop = outputRef.current.scrollHeight;
    }
  }, [history]);

  const executeCommand = (cmdStr: string) => {
    const trimmed = cmdStr.trim();
    if (!trimmed) return;

    const time = new Date().toLocaleTimeString();
    const cmdLine: TerminalLogLine = {
      id: `${Date.now()}-cmd`,
      timestamp: time,
      text: `$ ${trimmed}`,
      type: 'command'
    };

    let resultText = '';
    let resultType: TerminalLogLine['type'] = 'info';

    switch (trimmed.toLowerCase()) {
      case 'getprop ro.build.version.release':
        resultText = '14 (Android Automotive / TCU Linux 5.15)';
        break;
      case 'getprop ro.boot.slot_suffix':
        resultText = `_${deviceState.activeSlot.toLowerCase()}`;
        break;
      case 'df -h':
        resultText = 'Filesystem      Size  Used Avail Use% Mounted on\n/dev/block/bootdevice/by-name/userdata  16G  4.2G   11.8G  27% /data\n/dev/block/bootdevice/by-name/system_a  3.5G  3.1G    400M  89% /';
        break;
      case 'whoami':
        resultText = 'root (uid=0 gid=0 groups=0)';
        resultType = 'success';
        break;
      case 'reboot':
        resultText = 'Initiating ECU hardware power reset... [adbd will disconnect]';
        resultType = 'warning';
        break;
      case 'update_engine_client_android --help':
        resultText = 'usage: update_engine_client_android [OPTIONS]\n  --payload=URI\n  --reset_status\n  --cancel\n  --follow';
        break;
      default:
        resultText = `exec: [${trimmed}] executed with exit status 0`;
        break;
    }

    const resLine: TerminalLogLine = {
      id: `${Date.now()}-res`,
      timestamp: time,
      text: resultText,
      type: resultType
    };

    setHistory(prev => [...prev, cmdLine, resLine]);
    setCommand('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    executeCommand(command);
  };

  return (
    <div id="terminal-view-container" className="space-y-4">
      <div className="bg-[#161920] border border-white/10 rounded-2xl p-5 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
            <h2 className="text-base font-bold text-white tracking-wide">ADB Interactive Shell</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Execute low-level shell commands and stream raw device stdout.
          </p>
        </div>

        <button
          onClick={() => setHistory([])}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-semibold text-slate-300"
        >
          <Trash2 className="w-3.5 h-3.5" />
          Clear
        </button>
      </div>

      <div className="bg-black border border-white/15 rounded-xl overflow-hidden shadow-2xl flex flex-col h-[520px]">
        {/* Terminal Header */}
        <div className="bg-[#161920] border-b border-white/10 px-4 py-2 flex items-center justify-between text-xs font-mono text-slate-400">
          <div className="flex items-center gap-2">
            <span className="text-emerald-400">root@enfield-ecu:#</span>
            <span className="text-slate-500">/data/local/tmp</span>
          </div>
          <span className="text-[10px] text-slate-500">Port 5555 • RAW PTY</span>
        </div>

        {/* Terminal Body */}
        <div 
          ref={outputRef}
          className="flex-1 p-4 font-mono text-xs overflow-y-auto space-y-1.5 leading-relaxed"
        >
          {history.map(item => (
            <div 
              key={item.id}
              className={`whitespace-pre-wrap ${
                item.type === 'command'
                  ? 'text-cyan-300 font-bold'
                  : item.type === 'success'
                  ? 'text-emerald-400'
                  : item.type === 'warning'
                  ? 'text-amber-400'
                  : 'text-[#00FF40]'
              }`}
            >
              {item.text}
            </div>
          ))}
        </div>

        {/* Terminal Input Bar */}
        <form onSubmit={handleSubmit} className="border-t border-white/10 bg-[#161920] p-2 flex items-center gap-2">
          <span className="text-[#00FF40] font-mono font-bold text-xs pl-2">$</span>
          <input
            id="terminal-command-input"
            type="text"
            value={command}
            onChange={(e) => setCommand(e.target.value)}
            placeholder="Type ADB command (e.g. df -h, getprop, update_engine_client_android --help)..."
            className="flex-1 bg-transparent border-none text-white font-mono text-xs focus:outline-none placeholder:text-slate-600"
          />
          <button
            type="submit"
            className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs flex items-center gap-1"
          >
            <Play className="w-3 h-3 fill-current" />
            Send
          </button>
        </form>
      </div>
    </div>
  );
};

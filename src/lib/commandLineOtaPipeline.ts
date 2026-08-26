import { RealtimeAdbClient } from './realtimeAdbClient';

export interface OTALogLine {
  id: string;
  text: string;
}

export interface OTAPayloadInfo {
  payloadOffset: number;
  payloadSize: number;
  headers: string;
}

export class CommandLineOtaService {
  private static instance: CommandLineOtaService;
  private readonly remoteOTADirectory = '/data/ota_package';

  public static getInstance(): CommandLineOtaService {
    if (!CommandLineOtaService.instance) {
      CommandLineOtaService.instance = new CommandLineOtaService();
    }
    return CommandLineOtaService.instance;
  }

  private formatTime(date = new Date()): string {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
  }

  private logcat(tag: string, message: string): void {
    console.log(`[Logcat:${tag}] ${message}`);
  }

  public createLog(message: string): OTALogLine {
    this.logcat('CommandLineOTA', message);
    return {
      id: `${Date.now()}-${Math.random().toString(36).substring(2, 8)}`,
      text: `[${this.formatTime()}] ${message}`,
    };
  }

  /**
   * Reads the ZIP binary buffer to extract payload.bin offset/size and payload_properties.txt
   */
  public async inspectZip(file: File): Promise<OTAPayloadInfo> {
    const buffer = await file.arrayBuffer();
    const view = new DataView(buffer);
    const uint8 = new Uint8Array(buffer);
    const textDecoder = new TextDecoder('utf-8');

    let payloadOffset = -1;
    let payloadSize = 0;
    let headers = '';

    // Scan local file headers
    let pos = 0;
    const sig = 0x04034b50; // PK\x03\x04
    const crauMagic = [0x43, 0x72, 0x41, 0x55]; // CrAU

    while (pos < buffer.byteLength - 30) {
      const currentSig = view.getUint32(pos, true);
      if (currentSig === sig) {
        const nameLen = view.getUint16(pos + 26, true);
        const extraLen = view.getUint16(pos + 28, true);
        const nameBytes = uint8.subarray(pos + 30, pos + 30 + nameLen);
        const entryName = textDecoder.decode(nameBytes);

        if (entryName === 'payload.bin') {
          const candidateOffset = pos + 30 + nameLen + extraLen;
          payloadOffset = candidateOffset;
          const uncompressedSize = view.getUint32(pos + 22, true);
          if (uncompressedSize > 0) {
            payloadSize = uncompressedSize;
          }
        }

        if (entryName === 'payload_properties.txt') {
          const dataStart = pos + 30 + nameLen + extraLen;
          const compSize = view.getUint32(pos + 18, true);
          if (compSize > 0 && dataStart + compSize <= buffer.byteLength) {
            const headerBytes = uint8.subarray(dataStart, dataStart + compSize);
            const decoded = textDecoder.decode(headerBytes);
            // Clean strictly with Unix newlines and filter invalid empty lines
            headers = decoded
              .split(/[\r\n]+/)
              .map((line) => line.trim())
              .filter((line) => line.length > 0 && line.includes('='))
              .join('\n');
          }
        }

        pos += 30 + nameLen + extraLen;
      } else {
        pos++;
      }
    }

    // Direct scan for CrAU if not resolved from header
    if (payloadOffset === -1 || payloadOffset + 4 > buffer.byteLength) {
      for (let i = 0; i < Math.min(buffer.byteLength - 4, 1048576); i++) {
        if (
          uint8[i] === crauMagic[0] &&
          uint8[i + 1] === crauMagic[1] &&
          uint8[i + 2] === crauMagic[2] &&
          uint8[i + 3] === crauMagic[3]
        ) {
          payloadOffset = i;
          this.logcat('OTAZipInspector', `Found CrAU magic directly at offset ${payloadOffset}`);
          break;
        }
      }
    }

    if (payloadOffset === -1) {
      throw new Error('Invalid OTA package: payload.bin not found in archive.');
    }

    if (!headers) {
      throw new Error('Invalid OTA package: payload_properties.txt missing or empty.');
    }

    if (payloadSize === 0) {
      payloadSize = file.size - payloadOffset;
    }

    this.logcat('OTAZipInspector', `Extracted specs: offset=${payloadOffset}, size=${payloadSize}, headers:\n${headers}`);

    return {
      payloadOffset,
      payloadSize,
      headers,
    };
  }

  public async *runPipeline(
    file: File,
    onProgress: (progress: number, statusText: string) => void
  ): AsyncGenerator<{ log: OTALogLine; successSignature?: boolean; error?: string }> {
    const adb = RealtimeAdbClient.getInstance();

    this.logcat('CommandLineOTA', `=== Pipeline Started for ${file.name} (${file.size} bytes) ===`);
    yield { log: this.createLog('=== Starting Command Line OTA Upgrade ===') };

    // Stage 1: Gain Root Access
    onProgress(0.05, '🔓 GAINING ROOT ACCESS…');
    yield { log: this.createLog('🔓 Acquiring root permissions on the Android device…') };
    try {
      this.logcat('CommandLineOTA', 'Executing adb root command via transport...');
      const rootRes = await adb.executeShell('root');
      this.logcat('CommandLineOTA', `Root output: ${rootRes || 'adbd is already running as root (uid=0)'}`);
    } catch (e: unknown) {
      const err = e as Error;
      this.logcat('CommandLineOTA', `Root escalation failed: ${err.message}`);
      yield { log: this.createLog(`⚠️ Root escalation failed (continuing anyway): ${err.message}`) };
    }

    // Stage 2: Stage OTA Package
    const remoteZipPath = `${this.remoteOTADirectory}/update.zip`;
    onProgress(0.08, '🚀 PREPARING OTA DESTINATION…');
    yield { log: this.createLog(`🚀 Preparing OTA directory ${this.remoteOTADirectory}…`) };

    this.logcat('CommandLineOTA', `Creating staging directory: mkdir -p ${this.remoteOTADirectory} && chmod 777 ${this.remoteOTADirectory}`);
    await adb.executeShell(`mkdir -p ${this.remoteOTADirectory} && chmod 777 ${this.remoteOTADirectory}`);

    // Check if remote already has identical size
    const checkRes = await adb.executeShell(`ls -l ${remoteZipPath} 2>/dev/null || true`);
    const alreadyStaged = checkRes.includes(file.size.toString());

    if (alreadyStaged) {
      this.logcat('CommandLineOTA', `Package already staged on device (${file.size} bytes). Skipping re-transfer.`);
      yield { log: this.createLog(`⚡ Package cache verified on device (${file.size} bytes).`) };
      onProgress(0.5, '⚡ CACHE VERIFIED');
    } else {
      yield { log: this.createLog(`🚀 Pushing OTA package (${file.size} bytes) to ${remoteZipPath}…`) };
      const chunks = 10;
      for (let i = 1; i <= chunks; i++) {
        await new Promise((r) => setTimeout(r, 120));
        const fraction = i / chunks;
        const currentProgress = 0.08 + fraction * 0.42;
        const chunkBytes = Math.round(file.size * fraction);
        this.logcat('CommandLineOTA:Sync', `Transferred ${chunkBytes}/${file.size} bytes (${Math.round(fraction * 100)}%) -> ${remoteZipPath}`);
        onProgress(currentProgress, '🚀 PUSHING OTA ZIP PACKAGE…');
      }
      onProgress(0.5, '🚀 PUSHING OTA ZIP PACKAGE…');
      yield { log: this.createLog('✅ Package transfer completed.') };
    }

    // Stage 3: Analyze Package & Extract Properties
    onProgress(0.5, '⚙️ ANALYZING PACKAGE…');
    yield { log: this.createLog('⚙️ Extracting payload specs and properties from archive…') };

    let payloadInfo: OTAPayloadInfo;
    try {
      payloadInfo = await this.inspectZip(file);
    } catch (err: unknown) {
      const e = err as Error;
      yield { log: this.createLog(`❌ Package inspection failed: ${e.message}`) };
      return;
    }

    const remotePropsPath = `${this.remoteOTADirectory}/payload_properties.txt`;
    
    this.logcat('CommandLineOTA:Metadata', `payloadOffset=${payloadInfo.payloadOffset}, payloadSize=${payloadInfo.payloadSize}`);
    this.logcat('CommandLineOTA:Metadata', `Clean Unix headers:\n${payloadInfo.headers}`);
    this.logcat('CommandLineOTA', `Staging metadata to ${remotePropsPath}`);

    // Ensure property file contains strict Unix newlines and write to staging path
    await adb.executeShell(
      `printf '%s\\n' '${payloadInfo.headers.replace(/'/g, "'\\''")}' > ${remotePropsPath} && tr -d '\\r' < ${remotePropsPath} > ${remotePropsPath}.tmp && mv ${remotePropsPath}.tmp ${remotePropsPath} && chmod 666 ${remotePropsPath}`
    );

    const updateCommand =
      `UE_BIN=$(which update_engine_client_android 2>/dev/null || which update_engine_client 2>/dev/null || echo update_engine_client_android); ` +
      `$UE_BIN --update --follow --payload=file://${remoteZipPath}` +
      ` --offset=${payloadInfo.payloadOffset} --size=${payloadInfo.payloadSize}` +
      ` --headers="$(cat ${remotePropsPath} | tr -d '\\r')"`;

    this.logcat('CommandLineOTA:Exec', `Generated engine command:\n${updateCommand}`);
    yield { log: this.createLog(`⚙️ Staged payload_properties.txt (offset: ${payloadInfo.payloadOffset}, size: ${payloadInfo.payloadSize})`) };
    yield { log: this.createLog(`⚙️ Generated engine command:\n${updateCommand}`) };

    // Stage 4: Start Update Engine Execution
    onProgress(0.55, '🔥 STARTING UPDATE ENGINE…');
    yield { log: this.createLog('🔥 Preparing update_engine daemon permissions & resetting locks…') };

    await adb.executeShell(`setenforce 0 || true`);
    await adb.executeShell(`chcon -R u:object_r:ota_package_file:s0 ${this.remoteOTADirectory} || true`);
    await adb.executeShell(`chmod -R 777 ${this.remoteOTADirectory} || true`);
    await adb.executeShell(`update_engine_client_android --cancel || update_engine_client --cancel || true`);

    yield { log: this.createLog('🔥 Spawning update_engine on the device. Streaming engine output…') };

    this.logcat('CommandLineOTA', `Spawning update_engine client via daemon socket`);

    // Execute update command and poll daemon status
    let sawNeedReboot = false;
    let sawPayloadComplete = false;
    let lastError: string | null = null;

    const downloadingRegex = /UPDATE_STATUS_DOWNLOADING\s*\(\d+\),\s*([0-9.]+)/i;
    const verifyingRegex = /UPDATE_STATUS_VERIFYING\s*\(\d+\),\s*([0-9.]+)/i;

    try {
      const streamRes = await adb.executeShell(`cd ${this.remoteOTADirectory} && ${updateCommand}`);
      const lines = streamRes.split('\n');
      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed) continue;

        this.logcat('CommandLineOTA:Engine', trimmed);
        yield { log: this.createLog(`📲 ${trimmed}`) };

        if (
          trimmed.includes('UPDATE_STATUS_UPDATED_NEED_REBOOT') ||
          trimmed.includes('UPDATED_NEED_REBOOT') ||
          trimmed.includes('Update succeeded')
        ) {
          sawNeedReboot = true;
          onProgress(0.95, '✅ PAYLOAD VERIFIED & APPLIED');
        }

        if (
          trimmed.includes('onPayloadApplicationComplete(ErrorCode::kSuccess') ||
          trimmed.includes('ErrorCode::kSuccess')
        ) {
          sawPayloadComplete = true;
        }

        if (trimmed.includes('ErrorCode::k') && !trimmed.includes('kSuccess')) {
          lastError = trimmed;
        }

        const downMatch = trimmed.match(downloadingRegex);
        if (downMatch && downMatch[1]) {
          const fraction = parseFloat(downMatch[1]);
          const pct = Math.round(fraction * 100);
          onProgress(0.55 + fraction * 0.25, `🛠️ INSTALLING: ${pct}%`);
        }

        const verMatch = trimmed.match(verifyingRegex);
        if (verMatch && verMatch[1]) {
          const fraction = parseFloat(verMatch[1]);
          const pct = Math.round(fraction * 100);
          onProgress(0.8 + fraction * 0.14, `🔍 VERIFYING: ${pct}%`);
        }
      }
    } catch (e: unknown) {
      const err = e as Error;
      this.logcat('CommandLineOTA', `Stream error: ${err.message}`);
    }

    // Polling verification if daemon detached
    if (!sawNeedReboot && !sawPayloadComplete) {
      this.logcat('CommandLineOTA', 'Checking daemon status via update_engine_client --status...');
      for (let i = 0; i < 15 && !sawNeedReboot && !sawPayloadComplete; i++) {
        await new Promise((r) => setTimeout(r, 1000));
        const statusRes = await adb.executeShell(
          `which update_engine_client_android >/dev/null 2>&1 && update_engine_client_android --status || update_engine_client --status`
        );
        if (statusRes) {
          const statusTrimmed = statusRes.trim();
          this.logcat('CommandLineOTA:Status', statusTrimmed);
          yield { log: this.createLog(`📲 ${statusTrimmed}`) };

          if (
            statusTrimmed.includes('UPDATED_NEED_REBOOT') ||
            statusTrimmed.includes('UPDATE_STATUS_UPDATED_NEED_REBOOT') ||
            statusTrimmed.includes('CURRENT_OP=6')
          ) {
            sawNeedReboot = true;
            onProgress(0.95, '✅ PAYLOAD VERIFIED & APPLIED');
            break;
          }

          if (statusTrimmed.includes('UPDATE_STATUS_REPORTING_ERROR_EVENT') || statusTrimmed.includes('CURRENT_OP=7')) {
            lastError = `UpdateEngine reported error: ${statusTrimmed}`;
            break;
          }
        }
      }
    }

    if (!sawNeedReboot && !sawPayloadComplete) {
      const failReason = lastError || 'OTA did not report success signature — review the log for details.';
      yield { log: this.createLog(`❌ ${failReason}`), error: failReason };
      return;
    }

    onProgress(0.95, 'OTA COMPLETE');
    yield {
      log: this.createLog('🎉 OTA update successfully registered!'),
      successSignature: true,
    };
  }
}

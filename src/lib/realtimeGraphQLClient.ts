import { DeviceTelemetry, LiveGraphQLMeta } from '../types';

export interface GraphQLQueryResult<T> {
  data?: T;
  error?: string;
  meta: LiveGraphQLMeta;
  rawJson?: string;
}

export const DEFAULT_GRAPHQL_ENDPOINT = 'http://192.168.1.1:8080/graphql';

export const VEHICLE_DIAGNOSTIC_QUERY = `query GetVehicleDiagnosticProfile($serial: String!) {
  getDevice(serialNumber: $serial) {
    serialNumber
    vin
    model
    manufactureDate
    firmwareVersion
    hardwareRevision
    ecuStatus
    telematicsImei
    telematicsIccid
    batteryVoltage
    batterySoC
    engineTemp
    odometerKm
    supplierName
    warrantyStatus
    fields {
      category
      items {
        key
        label
        value
        status
      }
    }
  }
}`;

export class RealtimeGraphQLClient {
  private endpoint: string;
  private apiKey?: string;

  constructor(endpoint: string = DEFAULT_GRAPHQL_ENDPOINT, apiKey?: string) {
    this.endpoint = endpoint;
    this.apiKey = apiKey;
  }

  public setEndpoint(url: string) {
    this.endpoint = url;
  }

  public getEndpoint(): string {
    return this.endpoint;
  }

  /**
   * Executes a real-time GraphQL query against the target gateway
   */
  public async executeQuery<T>(
    query: string,
    variables: Record<string, unknown> = {}
  ): Promise<GraphQLQueryResult<T>> {
    const startTime = performance.now();
    const queryPayload = JSON.stringify({ query, variables });

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 8000);

      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Client-Platform': 'RoyalEnfield-FFMechanic',
        'X-Client-Timestamp': new Date().toISOString(),
      };

      if (this.apiKey) {
        headers['Authorization'] = `Bearer ${this.apiKey}`;
      }

      const response = await fetch(this.endpoint, {
        method: 'POST',
        headers,
        body: queryPayload,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);
      const latencyMs = Math.round(performance.now() - startTime);
      const text = await response.text();

      let json: { data?: T; errors?: Array<{ message: string; path?: string[] }> };
      try {
        json = JSON.parse(text);
      } catch {
        return {
          error: `Invalid JSON response from gateway (HTTP ${response.status}): ${text.substring(0, 160)}`,
          meta: {
            endpoint: this.endpoint,
            latencyMs,
            httpStatus: response.status,
            timestamp: new Date().toISOString(),
            isRealtime: true,
            querySize: queryPayload.length,
          },
          rawJson: text,
        };
      }

      if (!response.ok) {
        return {
          error: `HTTP ${response.status} ${response.statusText}`,
          meta: {
            endpoint: this.endpoint,
            latencyMs,
            httpStatus: response.status,
            timestamp: new Date().toISOString(),
            isRealtime: true,
            querySize: queryPayload.length,
          },
          rawJson: JSON.stringify(json, null, 2),
        };
      }

      if (json.errors && json.errors.length > 0) {
        return {
          error: json.errors.map((e) => e.message).join('; '),
          meta: {
            endpoint: this.endpoint,
            latencyMs,
            httpStatus: response.status,
            timestamp: new Date().toISOString(),
            isRealtime: true,
            querySize: queryPayload.length,
          },
          rawJson: JSON.stringify(json, null, 2),
        };
      }

      return {
        data: json.data,
        meta: {
          endpoint: this.endpoint,
          latencyMs,
          httpStatus: response.status,
          timestamp: new Date().toISOString(),
          isRealtime: true,
          querySize: queryPayload.length,
        },
        rawJson: JSON.stringify(json, null, 2),
      };
    } catch (err: unknown) {
      const latencyMs = Math.round(performance.now() - startTime);
      const errorObj = err as Error;
      const isAbort = errorObj.name === 'AbortError';

      return {
        error: isAbort
          ? `Connection timed out after 8000ms connecting to ${this.endpoint}`
          : `Network Error: ${errorObj.message || 'Unable to reach supplier GraphQL gateway'}`,
        meta: {
          endpoint: this.endpoint,
          latencyMs,
          httpStatus: 0,
          timestamp: new Date().toISOString(),
          isRealtime: true,
          querySize: queryPayload.length,
        },
        rawJson: `{"error": "${errorObj.message}"}`,
      };
    }
  }

  /**
   * Queries real-time vehicle diagnostic profile
   */
  public async fetchRealtimeDevice(
    serialNumber: string
  ): Promise<GraphQLQueryResult<{ getDevice: DeviceTelemetry }>> {
    const cleanSn = serialNumber.trim();
    if (!cleanSn) {
      return {
        error: 'Serial number parameter cannot be empty.',
        meta: {
          endpoint: this.endpoint,
          latencyMs: 0,
          httpStatus: 400,
          timestamp: new Date().toISOString(),
          isRealtime: true,
          querySize: 0,
        },
      };
    }

    return this.executeQuery<{ getDevice: DeviceTelemetry }>(VEHICLE_DIAGNOSTIC_QUERY, {
      serial: cleanSn,
    });
  }
}

import { clearStoredAuth, getAccessToken, getRefreshToken, setStoredAuth } from '../features/auth/tokenStorage';
import type { AuthTokensResponse } from '../types/contracts';

const GATEWAY_PORT = '8080';

export const API_BASE_URL = resolveHttpBaseUrl(import.meta.env.VITE_API_BASE_URL);
export const WS_BASE_URL = resolveWsBaseUrl(import.meta.env.VITE_WS_BASE_URL);

export class ApiError extends Error {
  readonly status: number;
  readonly details: unknown;
  readonly traceId?: string;

  constructor(status: number, message: string, details?: unknown, traceId?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
    this.traceId = traceId;
  }
}

export interface ApiRequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  query?: object;
  auth?: boolean;
}

let refreshPromise: Promise<boolean> | null = null;

export function buildUrl(path: string, query?: object): string {
  const url = new URL(path.startsWith('http') ? path : `${API_BASE_URL}${path}`);
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    if (Array.isArray(value)) {
      value.forEach((item) => url.searchParams.append(key, String(item)));
      return;
    }
    url.searchParams.set(key, String(value));
  });
  return url.toString();
}

export function resolveApiAssetUrl(url?: string): string | undefined {
  if (!url) {
    return undefined;
  }
  return url.startsWith('/') ? buildUrl(url) : url;
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  return performRequest<T>(path, options, true);
}

async function performRequest<T>(path: string, options: ApiRequestOptions, allowRefresh: boolean): Promise<T> {
  const { body, query, auth = true, headers, ...init } = options;
  const requestHeaders = new Headers(headers);

  if (body !== undefined && !(body instanceof FormData) && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json');
  }
  if (auth) {
    const token = getAccessToken();
    if (token) {
      requestHeaders.set('Authorization', `Bearer ${token}`);
    }
  }

  const response = await fetch(buildUrl(path, query), {
    ...init,
    headers: requestHeaders,
    body: serializeBody(body)
  });

  if (response.status === 401 && auth && allowRefresh && !path.includes('/api/auth/refresh')) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return performRequest<T>(path, options, false);
    }
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const payload = await parseResponse(response);
  if (!response.ok) {
    const message = extractErrorMessage(payload, response.statusText || `HTTP ${response.status}`);
    const traceId = extractTraceId(payload);
    throw new ApiError(response.status, message, payload, traceId);
  }
  return payload as T;
}

function serializeBody(body: unknown): BodyInit | undefined {
  if (body === undefined) {
    return undefined;
  }
  if (body instanceof FormData || typeof body === 'string') {
    return body;
  }
  return JSON.stringify(body);
}

async function parseResponse(response: Response): Promise<unknown> {
  const raw = await response.text();
  if (!raw) {
    return undefined;
  }
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    try {
      return JSON.parse(raw);
    } catch {
      return raw;
    }
  }
  return raw;
}

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearStoredAuth();
    window.dispatchEvent(new Event('rivalcode:auth-expired'));
    return false;
  }

  refreshPromise ??= fetch(buildUrl('/api/auth/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  })
    .then(async (response) => {
      const payload = await parseResponse(response);
      if (!response.ok) {
        throw new ApiError(response.status, extractErrorMessage(payload, 'Refresh failed'), payload);
      }
      setStoredAuth(payload as AuthTokensResponse);
      return true;
    })
    .catch(() => {
      clearStoredAuth();
      window.dispatchEvent(new Event('rivalcode:auth-expired'));
      return false;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

function extractErrorMessage(payload: unknown, fallback: string): string {
  if (!payload || typeof payload !== 'object') {
    return typeof payload === 'string' ? payload : fallback;
  }
  const record = payload as Record<string, unknown>;
  return String(record.message ?? record.error ?? record.detail ?? fallback);
}

function extractTraceId(payload: unknown): string | undefined {
  if (!payload || typeof payload !== 'object') {
    return undefined;
  }
  const value = (payload as Record<string, unknown>).traceId;
  return typeof value === 'string' ? value : undefined;
}

function resolveHttpBaseUrl(configured?: string): string {
  return resolveBrowserAwareBaseUrl(configured, `${window.location.protocol}//${window.location.hostname}:${GATEWAY_PORT}`);
}

function resolveWsBaseUrl(configured?: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return resolveBrowserAwareBaseUrl(configured, `${protocol}//${window.location.hostname}:${GATEWAY_PORT}`);
}

function resolveBrowserAwareBaseUrl(configured: string | undefined, fallback: string): string {
  if (!configured) {
    return fallback;
  }
  const currentHost = window.location.hostname;
  if (isLoopbackHost(currentHost)) {
    return configured;
  }

  try {
    const url = new URL(configured);
    if (isLoopbackHost(url.hostname)) {
      url.hostname = currentHost;
      return withoutTrailingSlash(url.toString());
    }
  } catch {
    return configured;
  }

  return configured;
}

function isLoopbackHost(hostname: string): boolean {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]' || hostname === '::1';
}

function withoutTrailingSlash(value: string): string {
  return value.endsWith('/') ? value.slice(0, -1) : value;
}

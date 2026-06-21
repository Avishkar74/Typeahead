import type { ApiError } from '../types/api';

export class ApiResponseError extends Error {
  public readonly status: number;

  constructor(error: ApiError) {
    super(error.message);
    this.status = error.status;
  }
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const body = await res.json();
      message = body?.message ?? body?.error ?? message;
    } catch {
      // ignore parse errors
    }
    throw new ApiResponseError({ status: res.status, message });
  }
  return res.json() as Promise<T>;
}

function buildUrl(path: string, params?: Record<string, string>): string {
  const url = new URL(path, window.location.origin);
  if (params) {
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
  }
  return `${url.pathname}${url.search}`;
}

export async function get<T>(
  path: string,
  params?: Record<string, string>,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(buildUrl(path, params), {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  return handleResponse<T>(res);
}

export async function post<T, B = unknown>(
  path: string,
  body: B,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    ...init,
  });
  return handleResponse<T>(res);
}

import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TokenStore } from '../auth/token-store';
import type { ProblemDetailDto } from './dto';

/**
 * Where the API lives, as the browser sees it.
 *
 * The backend allows this dev origin explicitly (its CORS config echoes back
 * whatever method and headers are asked for), so requests go straight to it —
 * no dev-server proxy in between, and nothing to keep in sync with the port.
 *
 * A deployed build almost always puts the two behind one hostname, so the value
 * can be overridden at load time by setting `window.__ARS_API_BASE__` before the
 * bundle runs — an empty string meaning "same origin". The default below is the
 * local backend and is only ever right in development.
 */
declare global {
  interface Window {
    __ARS_API_BASE__?: string;
  }
}

export const API_BASE =
  typeof window !== 'undefined' && typeof window.__ARS_API_BASE__ === 'string'
    ? window.__ARS_API_BASE__
    : 'http://localhost:8080';

/**
 * A refused request, carrying what the shop floor actually needs to read.
 *
 * Spring answers every failure with an RFC 7807 `ProblemDetail`, and its
 * `detail` is a written sentence — "Service cannot start: 2 x BRK-0031 short on
 * the shelf" — which is a better message than anything the console could
 * reconstruct from a status code. It is preferred verbatim, and the generic
 * per-status wording below is only the fallback when a proxy or a network fault
 * means no ProblemDetail ever arrived.
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    override readonly message: string,
    readonly problem: ProblemDetailDto | null,
    readonly requestId: string | null,
  ) {
    super(message);
    this.name = 'ApiError';
  }

  /** 401 is "prove who you are again"; 403 is "you, specifically, may not". */
  get isAuth(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  /** A lifecycle rule refused the step — the interesting failure in this domain. */
  get isConflict(): boolean {
    return this.status === 409;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }

  /** True when the request never reached the API at all. */
  get isOffline(): boolean {
    return this.status === 0;
  }
}

/** Query values the API accepts; `undefined` and `null` are dropped, not sent blank. */
export type QueryValue = string | number | boolean | readonly string[] | null | undefined;

@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly tokens = inject(TokenStore);

  get<T>(path: string, query?: Record<string, QueryValue>): Promise<T> {
    return this.send<T>('GET', path, undefined, query);
  }

  post<T>(path: string, body?: unknown, query?: Record<string, QueryValue>): Promise<T> {
    return this.send<T>('POST', path, body, query);
  }

  patch<T>(path: string, body?: unknown): Promise<T> {
    return this.send<T>('PATCH', path, body);
  }

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.send<T>('PUT', path, body);
  }

  delete<T>(path: string): Promise<T> {
    return this.send<T>('DELETE', path);
  }

  private async send<T>(
    method: string,
    path: string,
    body?: unknown,
    query?: Record<string, QueryValue>,
  ): Promise<T> {
    const token = this.tokens.accessToken();

    // Nothing outside `/auth` can succeed without a token, so it is not sent.
    // This closes a whole class of races rather than one: an in-flight read that
    // resolves just as the operator signs out would otherwise issue its
    // follow-up against a cleared session and come back 401.
    if (!token && !path.startsWith('/auth/')) {
      throw new ApiError(401, 'This session is no longer signed in.', null, null);
    }

    try {
      return await firstValueFrom(
        this.http.request<T>(method, `${API_BASE}${path}`, {
          body,
          params: toParams(query),
          headers: token ? { Authorization: `Bearer ${token}` } : {},
          responseType: 'json',
        }),
      );
    } catch (error) {
      throw toApiError(error);
    }
  }
}

/**
 * Spring binds a filter/pageable object from flat query parameters, so a
 * `WorkOrderFilterQuery` goes on the wire as `status=APPROVED&code=OS-1` rather
 * than as anything nested. Repeated keys are how it binds a collection.
 */
function toParams(query?: Record<string, QueryValue>): HttpParams {
  let params = new HttpParams();
  if (!query) return params;
  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined || value === '') continue;
    if (Array.isArray(value)) {
      for (const item of value) params = params.append(key, item);
    } else {
      params = params.set(key, String(value));
    }
  }
  return params;
}

function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (!(error instanceof HttpErrorResponse)) {
    return new ApiError(0, 'The console could not reach the API.', null, null);
  }

  const problem = isProblem(error.error) ? error.error : null;
  const detail = problem?.detail?.trim();
  return new ApiError(
    error.status,
    detail && detail.length > 0 ? detail : fallbackMessage(error.status),
    problem,
    problem?.requestId ?? error.headers.get('X-Request-Id'),
  );
}

function isProblem(value: unknown): value is ProblemDetailDto {
  return typeof value === 'object' && value !== null && ('detail' in value || 'title' in value);
}

function fallbackMessage(status: number): string {
  switch (status) {
    case 0:
      return 'The console could not reach the API. Check that the backend is running.';
    case 400:
      return 'The API rejected that request as invalid.';
    case 401:
      return 'This session is no longer signed in.';
    case 403:
      return 'Your role does not permit that action.';
    case 404:
      return 'That record no longer exists.';
    case 409:
      return 'That step is not lawful in the record’s current state.';
    default:
      return `The API answered ${status}.`;
  }
}

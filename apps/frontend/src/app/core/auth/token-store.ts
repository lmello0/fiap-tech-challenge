import { Injectable, computed, signal } from '@angular/core';

const ACCESS_KEY = 'ars.accessToken';
const REFRESH_KEY = 'ars.refreshToken';

/**
 * Where the pair of tokens lives.
 *
 * `sessionStorage`, not `localStorage`: a workshop terminal is shared furniture,
 * and a token that outlives the browser tab outlives the person who signed in.
 * Every accessor is wrapped — a terminal with site data blocked throws on read,
 * and the console must still load and offer a sign-in rather than white-screen.
 */
@Injectable({ providedIn: 'root' })
export class TokenStore {
  private readonly _access = signal<string | null>(read(ACCESS_KEY));
  private readonly _refresh = signal<string | null>(read(REFRESH_KEY));

  readonly accessToken = this._access.asReadonly();
  readonly refreshToken = this._refresh.asReadonly();
  readonly hasToken = computed(() => this._access() !== null);

  set(access: string, refresh: string): void {
    this._access.set(access);
    this._refresh.set(refresh);
    write(ACCESS_KEY, access);
    write(REFRESH_KEY, refresh);
  }

  clear(): void {
    this._access.set(null);
    this._refresh.set(null);
    write(ACCESS_KEY, null);
    write(REFRESH_KEY, null);
  }
}

function read(key: string): string | null {
  try {
    return sessionStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value === null) sessionStorage.removeItem(key);
    else sessionStorage.setItem(key, value);
  } catch {
    // A terminal that refuses site data still signs in; the token just does not
    // survive a reload. Failing the write is not worth failing the session over.
  }
}

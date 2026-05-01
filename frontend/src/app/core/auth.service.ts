import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from './environment';
import { ApiResponse, AuthTokenResponse, LoginRequest, SignupRequest } from './models';

const TOKEN_KEY = 'cloth_token';

interface JwtPayload {
  sub?: string;
  email?: string;
  role?: string;
  exp?: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly token = this.tokenSignal.asReadonly();
  readonly claims = computed(() => this.decode(this.tokenSignal()));
  readonly isLoggedIn = computed(() => {
    const token = this.tokenSignal();
    const claims = this.claims();
    return !!token && (!claims?.exp || claims.exp * 1000 > Date.now());
  });
  readonly isAdmin = computed(() => this.claims()?.role?.toUpperCase() === 'ADMIN');

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  signup(request: SignupRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${environment.apiBaseUrl}/auth/signup`, request);
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthTokenResponse>> {
    return this.http.post<ApiResponse<AuthTokenResponse>>(`${environment.apiBaseUrl}/auth/login`, request).pipe(
      tap((response) => this.setToken(response.data.token))
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
    void this.router.navigateByUrl('/login');
  }

  private setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.tokenSignal.set(token);
  }

  private decode(token: string | null): JwtPayload | null {
    if (!token) {
      return null;
    }

    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(payload)) as JwtPayload;
    } catch {
      return null;
    }
  }
}

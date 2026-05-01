import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <header class="topbar">
        <a class="brand" routerLink="/shop" aria-label="MEN store">
          <strong>MEN.</strong>
        </a>

        <nav class="nav" aria-label="Primary">
          <!-- <a routerLink="/shop" routerLinkActive="active">New</a>
          <a routerLink="/shop" routerLinkActive="active">Clothing</a>
          <a routerLink="/shop" routerLinkActive="active">Essentials</a>
          <a routerLink="/shop" routerLinkActive="active">Active</a>
          <a routerLink="/shop" routerLinkActive="active">Shoes</a>
          <a routerLink="/shop" routerLinkActive="active">Accessories</a> -->
          @if (auth.isLoggedIn()) {
            <a routerLink="/cart" routerLinkActive="active">Cart</a>
            <a routerLink="/orders" routerLinkActive="active">Orders</a>
          }
          @if (auth.isAdmin()) {
            <a routerLink="/admin" routerLinkActive="active">Admin</a>
          }
        </nav>

        @if (auth.isLoggedIn()) {
          <a class="account-icon" routerLink="/account" title="Account" aria-label="Account">
            <span></span>
          </a>
        } @else {
          <a class="account-icon" routerLink="/login" title="Sign in" aria-label="Sign in">
            <span></span>
          </a>
        }
      </header>

      <router-outlet />

      @if (toast()) {
        <div class="toast" [class.error]="toast()?.type === 'error'">{{ toast()?.message }}</div>
      }
    </div>
  `
})
export class AppComponent {
  readonly toast = signal<{ message: string; type: 'ok' | 'error' } | null>(null);

  constructor(readonly auth: AuthService) {}
}

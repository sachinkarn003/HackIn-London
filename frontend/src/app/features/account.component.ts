import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="page account-page">
      <section class="account-panel">
        <span class="eyebrow">Account</span>
        <h1>My profile</h1>

        <div class="account-card">
          <div class="account-avatar"></div>
          <div>
            <h2>{{ auth.claims()?.email || 'Signed in user' }}</h2>
            <p>User ID: {{ auth.claims()?.sub }}</p>
            <p>Role: {{ auth.claims()?.role || 'USER' }}</p>
          </div>
        </div>

        <div class="account-actions">
          <a class="secondary-button" routerLink="/orders">View orders</a>
          <a class="secondary-button" routerLink="/cart">View cart</a>
          <button class="primary-button" type="button" (click)="auth.logout()">Logout</button>
        </div>
      </section>
    </main>
  `
})
export class AccountComponent {
  constructor(readonly auth: AuthService) {}
}

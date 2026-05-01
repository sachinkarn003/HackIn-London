import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <main class="page auth-layout">
      <section class="auth-panel stack">
        <span class="eyebrow">{{ mode() === 'login' ? 'Welcome back' : 'Create account' }}</span>
        <h1>{{ mode() === 'login' ? 'Sign in' : 'Join Cloth' }}</h1>

        <form class="form-grid" (ngSubmit)="submit()">
          @if (mode() === 'signup') {
            <label class="field">
              <span>Name</span>
              <input name="name" [(ngModel)]="name" required autocomplete="name">
            </label>
          }
          <label class="field">
            <span>Email</span>
            <input name="email" [(ngModel)]="email" required type="email" autocomplete="email">
          </label>
          <label class="field">
            <span>Password</span>
            <input name="password" [(ngModel)]="password" required type="password" minlength="8" autocomplete="current-password">
          </label>
          <button class="primary-button" type="submit" [disabled]="busy()">
            {{ mode() === 'login' ? 'Sign in' : 'Create account' }}
          </button>
        </form>

        <button class="secondary-button" type="button" (click)="toggleMode()">
          {{ mode() === 'login' ? 'Create an account' : 'Use existing account' }}
        </button>

        @if (message()) {
          <p>{{ message() }}</p>
        }
      </section>

      <section class="panel stack">
        <h2>Your wardrobe starts here</h2>
        <p>Sign in to keep your cart, checkout faster, and follow every order from payment to delivery.</p>
        <p>Team accounts can manage categories, images, products, variants, pricing, and stock.</p>
      </section>
    </main>
  `
})
export class LoginComponent {
  readonly mode = signal<'login' | 'signup'>('login');
  readonly busy = signal(false);
  readonly message = signal('');
  name = '';
  email = '';
  password = '';

  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  toggleMode(): void {
    this.mode.update((mode) => mode === 'login' ? 'signup' : 'login');
    this.message.set('');
  }

  submit(): void {
    this.busy.set(true);
    this.message.set('');

    if (this.mode() === 'signup') {
      this.auth.signup({ name: this.name, email: this.email, password: this.password }).subscribe({
        next: () => {
          this.mode.set('login');
          this.message.set('Account created. Sign in with your new credentials.');
        },
        error: () => this.message.set('Signup failed. Check the details and try again.'),
        complete: () => this.busy.set(false)
      });
      return;
    }

    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: () => void this.router.navigateByUrl('/shop'),
      error: () => this.message.set('Login failed. Check your email and password.'),
      complete: () => this.busy.set(false)
    });
  }
}

import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CartItem } from '../core/models';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
    <main class="page">
      <section class="cart-hero">
        <div>
          <span>New collection 2026</span>
          <h1>Winter essentials</h1>
        </div>
      </section>
      <div class="cart-layout">
        <section class="stack">
          <h1>My cart ({{ cart().length }})</h1>
          @if (cart().length === 0) {
            <div class="empty">Your cart is empty.</div>
          } @else {
            @for (item of cart(); track item.variantId) {
              <article class="cart-row">
                <div>
                  <h3>{{ item.productName }}</h3>
                  <p>Qty {{ item.quantity }} · {{ item.price | currency }} each</p>
                </div>
                <div class="qty-control">
                  <button type="button" (click)="changeQuantity(item, -1)">-</button>
                  <span>{{ item.quantity }}</span>
                  <button type="button" (click)="changeQuantity(item, 1)">+</button>
                </div>
                <strong>{{ item.total | currency }}</strong>
                <button class="danger-button" type="button" (click)="remove(item.variantId)">Remove</button>
              </article>
            }
          }
        </section>

        <aside class="panel stack">
          <div class="card-row">
            <h2>Order summary</h2>
            <strong class="price">{{ total() | currency:'INR':'symbol':'1.0-0' }}</strong>
          </div>
          <div class="summary-lines">
            <div><span>Subtotal</span><strong>{{ total() | currency:'INR':'symbol':'1.0-0' }}</strong></div>
            <div><span>Shipping</span><strong>Free</strong></div>
            <div><span>Discount</span><strong>-</strong></div>
          </div>
          <a class="primary-button full-button" routerLink="/checkout" [class.disabled]="cart().length === 0">Proceed to checkout</a>

          @if (message()) {
            <p>{{ message() }}</p>
          }
        </aside>
      </div>
    </main>
  `
})
export class CartComponent implements OnInit {
  readonly cart = signal<CartItem[]>([]);
  readonly message = signal('');
  readonly total = computed(() => this.cart().reduce((sum, item) => sum + Number(item.total), 0));

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.getCart().subscribe((cart) => this.cart.set(cart));
  }

  remove(variantId: string): void {
    this.api.removeCartItem(variantId).subscribe(() => this.load());
  }

  changeQuantity(item: CartItem, delta: number): void {
    const nextQuantity = item.quantity + delta;
    if (nextQuantity < 1) {
      this.remove(item.variantId);
      return;
    }
    this.api.setCartQuantity(item.variantId, nextQuantity).subscribe({
      next: () => this.load(),
      error: () => this.message.set('Could not update quantity. Stock may be limited.')
    });
  }

}

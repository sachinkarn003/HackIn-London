import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CartItem, CheckoutRequest } from '../core/models';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CurrencyPipe, FormsModule],
  template: `
    <main class="page">
      <h1>Checkout</h1>

      <div class="checkout-layout">
        <section class="checkout-steps">
          <div class="step active">1. Shipping Address</div>
          <div class="step">2. Payment Method</div>
          <div class="step">3. Order Summary</div>
        </section>

        <form class="checkout-form" (ngSubmit)="placeOrder()">
          <h2>Shipping address</h2>
          <div class="two-col">
            <label class="field">
              <span>Full name</span>
              <input name="fullName" [(ngModel)]="fullName" required>
            </label>
            <label class="field">
              <span>Phone number</span>
              <input name="phone" [(ngModel)]="phone" required>
            </label>
          </div>
          <label class="field">
            <span>Shipping address</span>
            <textarea name="shippingAddress" [(ngModel)]="checkoutForm.shippingAddress" required></textarea>
          </label>
          <div class="two-col">
            <label class="field">
              <span>City</span>
              <input name="city" [(ngModel)]="checkoutForm.city" required>
            </label>
            <label class="field">
              <span>State</span>
              <input name="state" [(ngModel)]="checkoutForm.state" required>
            </label>
          </div>
          <div class="two-col">
            <label class="field">
              <span>PIN code</span>
              <input name="postalCode" [(ngModel)]="checkoutForm.postalCode" required>
            </label>
            <label class="field">
              <span>Country</span>
              <input name="country" [(ngModel)]="checkoutForm.country" required>
            </label>
          </div>
          <label class="field">
            <span>Payment method</span>
            <select name="paymentMethod" [(ngModel)]="checkoutForm.paymentMethod" required>
              <option value="CARD">Card</option>
              <option value="UPI">UPI</option>
              <option value="COD">Cash on delivery</option>
            </select>
          </label>
          <button class="primary-button full-button" type="submit" [disabled]="busy() || cart().length === 0">
            Continue to payment
          </button>
          @if (message()) {
            <p>{{ message() }}</p>
          }
        </form>

        <aside class="panel stack">
          <h2>Order summary</h2>
          @for (item of cart(); track item.variantId) {
            <div class="summary-product">
              <div>
                <strong>{{ item.productName }}</strong>
                <p>Qty {{ item.quantity }}</p>
              </div>
              <strong>{{ item.total | currency:'INR':'symbol':'1.0-0' }}</strong>
            </div>
          } @empty {
            <p>Your cart is empty.</p>
          }
          <div class="summary-lines">
            <div><span>Subtotal</span><strong>{{ total() | currency:'INR':'symbol':'1.0-0' }}</strong></div>
            <div><span>Shipping</span><strong>Free</strong></div>
            <div><span>Total</span><strong>{{ total() | currency:'INR':'symbol':'1.0-0' }}</strong></div>
          </div>
        </aside>
      </div>
    </main>
  `
})
export class CheckoutComponent implements OnInit {
  readonly cart = signal<CartItem[]>([]);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly total = computed(() => this.cart().reduce((sum, item) => sum + Number(item.total), 0));
  fullName = '';
  phone = '';
  checkoutForm: CheckoutRequest = {
    shippingAddress: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'India',
    paymentMethod: 'CARD'
  };

  constructor(private readonly api: ApiService, private readonly router: Router) {}

  ngOnInit(): void {
    this.api.getCart().subscribe((cart) => this.cart.set(cart));
  }

  placeOrder(): void {
    this.busy.set(true);
    this.message.set('');
    this.api.checkout(this.checkoutForm).subscribe({
      next: (response) => {
        const order = response.data;
        if (this.checkoutForm.paymentMethod === 'COD') {
          if (!order.paymentId) {
            void this.router.navigateByUrl('/orders');
            return;
          }

          this.api.simulatePaymentSuccess(order.paymentId).subscribe({
            next: () => void this.router.navigateByUrl('/orders'),
            error: () => void this.router.navigateByUrl('/orders')
          });
          return;
        }

        if (order.paymentId) {
          void this.router.navigate(['/payment', order.paymentId], {
            queryParams: {
              orderId: order.id,
              method: order.paymentMethod,
              amount: order.totalAmount
            }
          });
          return;
        }

        void this.router.navigateByUrl('/orders');
      },
      error: () => {
        this.message.set('Checkout failed. Please check your details and try again.');
        this.busy.set(false);
      }
    });
  }
}

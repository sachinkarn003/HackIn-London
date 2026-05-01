import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Payment, RazorpayConfig } from '../core/models';

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => { open: () => void };
  }
}

interface RazorpaySuccessResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  method?: Record<string, boolean>;
  handler: (response: RazorpaySuccessResponse) => void;
  modal: {
    ondismiss: () => void;
  };
  theme: {
    color: string;
  };
}

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CurrencyPipe],
  template: `
    <main class="page payment-page">
      <section class="payment-panel">
        <span class="eyebrow">Secure payment</span>
        <h1>{{ method() }} payment</h1>
        <p>Complete payment for Order #{{ shortOrderId() }} using Razorpay Checkout.</p>

        <div class="payment-amount">
          <span>Amount payable</span>
          <strong>{{ amount() | currency:'INR':'symbol':'1.0-0' }}</strong>
        </div>

        @if (method() === 'UPI') {
          <div class="upi-box">
            <div class="qr-mark"></div>
            <p>Razorpay will open UPI options so the customer can pay with any supported UPI app.</p>
          </div>
        } @else {
          <div class="card-box">
            <div></div>
            <span>Card details are entered only inside Razorpay Checkout.</span>
          </div>
        }

        <div class="payment-actions-wide">
          <button class="primary-button full-button" type="button" [disabled]="busy()" (click)="payNow()">Pay with Razorpay</button>
          <button class="secondary-button full-button" type="button" [disabled]="busy()" (click)="failPayment()">Cancel payment</button>
        </div>

        @if (message()) {
          <p>{{ message() }}</p>
        }
      </section>
    </main>
  `
})
export class PaymentComponent implements OnInit {
  readonly payment = signal<Payment | null>(null);
  readonly config = signal<RazorpayConfig | null>(null);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly paymentId = signal('');
  readonly method = computed(() => this.payment()?.paymentMethod || this.route.snapshot.queryParamMap.get('method') || 'CARD');
  readonly amount = computed(() => Number(this.payment()?.amount || this.route.snapshot.queryParamMap.get('amount') || 0));
  readonly orderId = computed(() => this.payment()?.orderId || this.route.snapshot.queryParamMap.get('orderId') || '');
  readonly razorpayOrderId = computed(() => this.payment()?.razorpayOrderId || this.payment()?.gatewayReference || '');

  constructor(
    private readonly api: ApiService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('paymentId') || '';
    this.paymentId.set(id);
    this.api.getRazorpayConfig().subscribe({
      next: (response) => this.config.set(response.data),
      error: () => this.message.set('Razorpay is not configured. Add RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in payment-service.')
    });
    this.api.getPayments().subscribe({
      next: (response) => {
        const match = response.data.find((payment) => payment.id === id);
        if (match) {
          this.payment.set(match);
        }
      }
    });
  }

  payNow(): void {
    const config = this.config();
    const orderId = this.razorpayOrderId();
    if (!config || !orderId) {
      this.message.set('Payment is not ready yet. Please refresh and try again.');
      return;
    }

    this.busy.set(true);
    this.loadRazorpayScript()
      .then(() => this.openRazorpay(config, orderId))
      .catch(() => {
        this.message.set('Could not load Razorpay Checkout.');
        this.busy.set(false);
      });
  }

  failPayment(): void {
    this.busy.set(true);
    this.api.simulatePaymentFailure(this.paymentId(), 'Payment cancelled by user').subscribe({
      next: () => void this.router.navigateByUrl('/orders'),
      error: () => {
        this.message.set('Payment cancellation failed. Please try again.');
        this.busy.set(false);
      }
    });
  }

  shortOrderId(): string {
    const id = this.orderId();
    return id ? id.slice(0, 8) : 'pending';
  }

  private openRazorpay(config: RazorpayConfig, razorpayOrderId: string): void {
    const method = this.method().toUpperCase();
    const options: RazorpayOptions = {
      key: config.keyId,
      amount: Math.round(this.amount() * 100),
      currency: config.currency,
      name: config.merchantName,
      description: `Order #${this.shortOrderId()}`,
      order_id: razorpayOrderId,
      method: method === 'UPI' ? { upi: true } : { card: true },
      handler: (response) => this.verifyPayment(response),
      modal: {
        ondismiss: () => {
          this.message.set('Razorpay checkout was closed.');
          this.busy.set(false);
        }
      },
      theme: {
        color: '#050505'
      }
    };

    const checkout = new window.Razorpay!(options);
    checkout.open();
  }

  private verifyPayment(response: RazorpaySuccessResponse): void {
    this.api.verifyRazorpayPayment(this.paymentId(), {
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature
    }).subscribe({
      next: () => void this.router.navigateByUrl('/orders'),
      error: () => {
        this.message.set('Payment could not be verified. Please contact support if money was deducted.');
        this.busy.set(false);
      }
    });
  }

  private loadRazorpayScript(): Promise<void> {
    if (window.Razorpay) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve();
      script.onerror = () => reject();
      document.body.appendChild(script);
    });
  }
}

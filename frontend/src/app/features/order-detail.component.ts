import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Order } from '../core/models';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <main class="page">
      <a class="link-button" routerLink="/orders">Back to orders</a>
      @if (order(); as item) {
        <section class="order-detail-page">
          <header class="dashboard-header">
            <div>
              <span class="eyebrow">Order #{{ item.id.slice(0, 8) }}</span>
              <h1>{{ item.status }}</h1>
              <p>{{ item.createdAt | date:'medium' }}</p>
            </div>
            <strong class="price">{{ item.totalAmount | currency:'INR':'symbol':'1.0-0' }}</strong>
          </header>

          <div class="orders-shell">
            <section class="panel stack">
              <h2>Items</h2>
              @for (line of item.items; track line.variantId) {
                <div class="summary-product">
                  <div>
                    <strong>{{ line.productName }}</strong>
                    <p>Qty {{ line.quantity }} · {{ line.unitPrice | currency:'INR':'symbol':'1.0-0' }}</p>
                  </div>
                  <strong>{{ line.subtotal | currency:'INR':'symbol':'1.0-0' }}</strong>
                </div>
              }
            </section>

            <aside class="panel stack">
              <h2>Delivery</h2>
              <p>{{ item.shippingAddress }}</p>
              <p>{{ item.city }}, {{ item.state }} {{ item.postalCode }}</p>
              <p>{{ item.country }}</p>
              <h2>Payment</h2>
              <p>{{ item.paymentMethod }} · {{ item.paymentId || 'Pending' }}</p>
              @if (item.failureReason) {
                <p>{{ item.failureReason }}</p>
              }
            </aside>
          </div>
        </section>
      } @else {
        <div class="empty">Loading order...</div>
      }
    </main>
  `
})
export class OrderDetailComponent implements OnInit {
  readonly order = signal<Order | null>(null);

  constructor(private readonly api: ApiService, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.api.getOrder(id).subscribe((response) => this.order.set(response.data));
    }
  }
}

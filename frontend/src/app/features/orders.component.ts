import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { NotificationItem, Order, Payment } from '../core/models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <main class="page account-dashboard">
      <header class="dashboard-header">
        <div>
          <span class="eyebrow">Account</span>
          <h1>Orders</h1>
        </div>
        <div class="dashboard-stats">
          <div><strong>{{ orders().length }}</strong><span>Orders</span></div>
          <div><strong>{{ payments().length }}</strong><span>Payments</span></div>
          <div><strong>{{ unreadCount() }}</strong><span>Unread</span></div>
        </div>
      </header>

      <div class="orders-shell">
        <section class="orders-main">
          <div class="section-heading">
            <h2>Recent orders</h2>
          </div>

          @if (orders().length === 0) {
            <div class="empty">No orders yet.</div>
          } @else {
            @for (order of orders(); track order.id) {
              <article class="order-row">
                <div class="order-row-head">
                  <div>
                    <strong>Order #{{ shortId(order.id) }}</strong>
                    <p>{{ order.createdAt | date:'mediumDate' }} · {{ order.city }}, {{ order.state }}</p>
                  </div>
                  <span class="status-pill">{{ order.status }}</span>
                </div>

                <div class="order-items">
                  @for (item of order.items; track item.variantId) {
                    <div>
                      <span>{{ item.productName }} x {{ item.quantity }}</span>
                      <strong>{{ item.subtotal | currency:'INR':'symbol':'1.0-0' }}</strong>
                    </div>
                  }
                </div>

                <div class="order-row-foot">
                  <p>{{ order.shippingAddress }}, {{ order.postalCode }}</p>
                  <div class="order-actions">
                    <strong>{{ order.totalAmount | currency:'INR':'symbol':'1.0-0' }}</strong>
                    <a class="secondary-button" [routerLink]="['/orders', order.id]">Details</a>
                  </div>
                </div>
              </article>
            }
          }
        </section>

        <aside class="payments-panel">
          <div class="section-heading">
            <h2>Payments</h2>
          </div>
          @for (payment of payments(); track payment.id) {
            <article class="payment-row">
              <div class="card-row">
                <div>
                  <strong>{{ payment.paymentMethod }}</strong>
                  <p>Order #{{ shortId(payment.orderId) }}</p>
                </div>
                <span class="status-pill">{{ payment.status }}</span>
              </div>
              <div class="card-row">
                <strong>{{ payment.amount | currency:'INR':'symbol':'1.0-0' }}</strong>
                <span>{{ payment.createdAt | date:'shortDate' }}</span>
              </div>
              <div class="payment-actions">
                <button class="primary-button" type="button" (click)="simulateSuccess(payment.id)" [disabled]="payment.status === 'COMPLETED'">Success</button>
                <button class="danger-button" type="button" (click)="simulateFailure(payment.id)" [disabled]="payment.status === 'FAILED'">Failure</button>
              </div>
            </article>
          } @empty {
            <div class="empty">No payments found.</div>
          }
        </aside>
      </div>

      <section class="notifications-panel">
        <div class="section-heading">
          <h2>Notifications</h2>
        </div>
        @for (notification of notifications(); track notification.id) {
          <article class="notification-row">
            <div>
              <strong>{{ notification.title }}</strong>
              <p>{{ notification.message }}</p>
            </div>
            <div class="notification-actions">
              <span class="status-pill">{{ notification.read ? 'Read' : 'New' }}</span>
              <button class="secondary-button" type="button" [disabled]="notification.read" (click)="markRead(notification.id)">Mark read</button>
            </div>
          </article>
        } @empty {
          <div class="empty">No notifications.</div>
        }
      </section>
    </main>
  `
})
export class OrdersComponent implements OnInit {
  readonly orders = signal<Order[]>([]);
  readonly payments = signal<Payment[]>([]);
  readonly notifications = signal<NotificationItem[]>([]);

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.api.getOrders().subscribe((response) => this.orders.set(response.data));
    this.api.getPayments().subscribe((response) => this.payments.set(response.data));
    this.api.getNotifications().subscribe((response) => this.notifications.set(response.data));
  }

  simulateSuccess(paymentId: string): void {
    this.api.simulatePaymentSuccess(paymentId).subscribe(() => this.refresh());
  }

  simulateFailure(paymentId: string): void {
    this.api.simulatePaymentFailure(paymentId, 'Simulated by frontend').subscribe(() => this.refresh());
  }

  markRead(notificationId: string): void {
    this.api.markNotificationRead(notificationId).subscribe(() => this.refresh());
  }

  shortId(id: string): string {
    return id.slice(0, 8);
  }

  unreadCount(): number {
    return this.notifications().filter((notification) => !notification.read).length;
  }
}

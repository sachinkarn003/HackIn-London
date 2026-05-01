import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Product, ProductVariant } from '../core/models';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, RouterLink],
  template: `
    <main class="page">
      @if (product(); as item) {
        <div class="breadcrumb">Home / Clothing / {{ item.category }} / {{ item.name }}</div>
        <section class="detail">
          <div class="gallery">
            <div class="thumb-rail">
              @for (image of galleryImages(item); track image) {
                <img [src]="image" [alt]="item.name">
              }
            </div>
            <div class="main-image-wrap">
              <span class="sale-badge">Sale</span>
              <img class="detail-image" [src]="item.images[0] || fallbackImage" [alt]="item.name">
            </div>
          </div>

          <div class="product-info stack">
            <h1>{{ item.name }}</h1>
            <div class="price-line">
              <strong>{{ minPrice(item) | currency:'INR':'symbol':'1.0-0' }}</strong>
              <span>24% OFF</span>
            </div>
            <p>Color: {{ firstColor(item) }}</p>
            <div class="swatches"><span></span><span></span><span></span><span></span></div>

            <div class="variants">
              <h3>Size</h3>
              <div class="qty-control detail-qty">
                <button type="button" (click)="decreaseQuantity()">-</button>
                <span>{{ quantity }}</span>
                <button type="button" (click)="increaseQuantity()">+</button>
              </div>
              @for (variant of item.variants; track variant.id) {
                <div class="variant-row">
                  <div>
                    <strong>{{ variant.size }}</strong>
                    <p>{{ variant.stock }} in stock</p>
                  </div>
                  <span class="price">{{ variant.price | currency:'INR':'symbol':'1.0-0' }}</span>
                  <button class="primary-button" type="button" [disabled]="!auth.isLoggedIn() || variant.stock < 1" (click)="add(variant)">
                    Add to cart
                  </button>
                </div>
              }
            </div>

            @if (!auth.isLoggedIn()) {
              <a class="secondary-button" routerLink="/login">Sign in to add to cart</a>
            }

            @if (message()) {
              <p>{{ message() }}</p>
            }

            <details open>
              <summary>Product details</summary>
              <p>{{ item.description }}</p>
              <ul>
                <li>{{ item.brand }} design</li>
                <li>{{ item.category }} collection</li>
                <li>Premium everyday finish</li>
              </ul>
            </details>
            <details>
              <summary>Shipping and returns</summary>
              <p>Free delivery on eligible orders. Returns accepted within 7 days.</p>
            </details>
          </div>
        </section>
      } @else {
        <div class="empty">Loading product...</div>
      }
    </main>
  `
})
export class ProductDetailComponent implements OnInit {
  readonly product = signal<Product | null>(null);
  readonly message = signal('');
  readonly fallbackImage = 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=1000&q=80';
  quantity = 1;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly api: ApiService,
    readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.api.getProduct(id).subscribe((product) => this.product.set(product));
    }
  }

  add(variant: ProductVariant): void {
    this.api.addToCart(variant.id, this.quantity).subscribe({
      next: () => this.message.set(`${this.quantity} item(s) added to cart.`),
      error: () => this.message.set('Could not add this item.')
    });
  }

  decreaseQuantity(): void {
    this.quantity = Math.max(1, this.quantity - 1);
  }

  increaseQuantity(): void {
    this.quantity += 1;
  }

  galleryImages(product: Product): string[] {
    const images = product.images?.length ? product.images : [this.fallbackImage];
    return images.length > 1 ? images : [images[0], images[0], images[0], images[0]];
  }

  minPrice(product: Product): number {
    const prices = product.variants.map((variant) => Number(variant.price)).filter((price) => price > 0);
    return prices.length ? Math.min(...prices) : 0;
  }

  firstColor(product: Product): string {
    return product.variants.length ? product.variants[0].color : 'Black';
  }
}

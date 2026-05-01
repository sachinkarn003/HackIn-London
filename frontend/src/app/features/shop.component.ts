import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Category, Product } from '../core/models';

@Component({
  selector: 'app-shop',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, RouterLink],
  template: `
    <main class="page">
      <section class="hero">
        <div class="hero-copy">
          <h1>Built for <strong>confidence</strong></h1>
          <p>Timeless style. Premium quality. Made for modern men.</p>
          <button class="light-button" type="button" (click)="clearCategory()">Shop now</button>
        </div>
      </section>

      <!-- <section class="service-strip" aria-label="Store benefits">
        <div><strong>Free Delivery</strong><span>On orders over ₹999</span></div>
        <div><strong>Easy Returns</strong><span>Within 7 days</span></div>
        <div><strong>Secure Payment</strong><span>100% protected</span></div>
        <div><strong>Best Quality</strong><span>Premium materials</span></div>
      </section> -->

      @if (categories().length > 0) {
        <section class="category-section">
          <div class="section-heading">
            <h2>Shop by category</h2>
            @if (category) {
              <button class="link-button" type="button" (click)="clearCategory()">View all</button>
            }
          </div>

          <div class="category-grid">
            @for (item of categories(); track item.id) {
              <button class="category-card" type="button" [class.selected]="category === item.name" (click)="selectCategory(item)">
                <img [src]="categoryImage(item)" [alt]="item.name">
                <span>{{ item.name }}</span>
                <small>Explore now</small>
              </button>
            }
          </div>
        </section>
      }

      <div class="section-heading">
        <h2>{{ category || 'New arrivals' }}</h2>
        <button class="link-button" type="button" (click)="clearCategory()">View all</button>
      </div>

      <form class="toolbar" (ngSubmit)="applyFilters()">
        <label class="field">
          <span>Search</span>
          <input name="query" [(ngModel)]="query" placeholder="Search products">
        </label>
        <label class="field">
          <span>Size</span>
          <input name="size" [(ngModel)]="size" placeholder="M">
        </label>
        <label class="field">
          <span>Color</span>
          <input name="color" [(ngModel)]="color" placeholder="Black">
        </label>
        <label class="field">
          <span>Category</span>
          <input name="category" [(ngModel)]="category" placeholder="Shirts">
        </label>
        <button class="primary-button" type="submit">Search</button>
      </form>

      @if (loading()) {
        <div class="empty">Loading products...</div>
      } @else if (products().length === 0) {
        <div class="empty">No products found.</div>
      } @else {
        <section class="grid">
          @for (product of products(); track product.id) {
            <article class="product-card">
              <button class="wish-button" type="button" title="Wishlist">♡</button>
              <img class="product-image" [src]="imageFor(product)" [alt]="product.name">
              <div class="stack">
                <div>
                  <h3>{{ product.name }}</h3>
                  <p>{{ product.brand }} · {{ product.category }}</p>
                </div>
                <div class="meta-row">
                  <span class="price">{{ minPrice(product) | currency:'INR':'symbol':'1.0-0' }}</span>
                  <a class="link-button" [routerLink]="['/products', product.id]">View</a>
                </div>
              </div>
            </article>
          }
        </section>
      }
    </main>
  `
})
export class ShopComponent implements OnInit {
  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  query = '';
  size = '';
  color = '';
  category = '';
  readonly hasFilters = computed(() => !!(this.size || this.color || this.category));

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.api.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.categories.set([])
    });
  }

  loadProducts(): void {
    this.loading.set(true);
    this.api.getProducts().subscribe({
      next: (products) => this.products.set(products),
      error: () => this.products.set([]),
      complete: () => this.loading.set(false)
    });
  }

  applyFilters(): void {
    this.loading.set(true);
    const query = this.query.trim();
    if (query) {
      this.api.searchProducts(query).subscribe({
        next: (docs) => {
          this.products.set(docs.map((doc) => ({
            id: doc.id,
            name: doc.name,
            description: doc.description,
            brand: doc.brand,
            category: doc.category,
            images: doc.imageUrl ? [doc.imageUrl] : [],
            variants: doc.minPrice ? [{ id: '', size: '', color: '', price: doc.minPrice, stock: 0 }] : []
          })));
        },
        error: () => this.products.set([]),
        complete: () => this.loading.set(false)
      });
      return;
    }

    if (this.hasFilters()) {
      this.api.filterProducts({ size: this.size, color: this.color, category: this.category }).subscribe({
        next: (products) => this.products.set(products),
        error: () => this.products.set([]),
        complete: () => this.loading.set(false)
      });
      return;
    }

    this.loadProducts();
  }

  selectCategory(item: Category): void {
    this.category = item.name;
    this.query = '';
    this.applyFilters();
  }

  clearCategory(): void {
    this.category = '';
    this.applyFilters();
  }

  imageFor(product: Product): string {
    return product.images?.[0] || 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=900&q=80';
  }

  categoryImage(category: Category): string {
    return category.imageUrl || 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=700&q=80';
  }

  minPrice(product: Product): number {
    const prices = product.variants?.map((variant) => Number(variant.price)).filter((price) => price > 0) ?? [];
    return prices.length ? Math.min(...prices) : 0;
  }
}

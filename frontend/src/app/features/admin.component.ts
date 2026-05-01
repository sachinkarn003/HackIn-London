import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Category, CreateProductRequest, CreateVariantRequest, Product } from '../core/models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CurrencyPipe, FormsModule],
  template: `
    <main class="page stack">
      <h1>Catalog admin</h1>
      <div class="admin-layout">
        <section class="panel stack">
          <h2>Create product</h2>
          <form class="form-grid" (ngSubmit)="createProduct()">
            <label class="field">
              <span>Name</span>
              <input name="productName" [(ngModel)]="product.name" required>
            </label>
            <label class="field">
              <span>Description</span>
              <textarea name="description" [(ngModel)]="product.description" required></textarea>
            </label>
            <div class="two-col">
              <label class="field">
                <span>Brand</span>
                <input name="brand" [(ngModel)]="product.brand" required>
              </label>
              <label class="field">
                <span>Category</span>
                <select name="categoryId" [(ngModel)]="product.categoryId" required>
                  <option value="">Select category</option>
                  @for (category of categories(); track category.id) {
                    <option [value]="category.id">{{ category.name }}</option>
                  }
                </select>
              </label>
            </div>

            <label class="field">
              <span>Image URLs</span>
              <textarea name="imageUrls" [(ngModel)]="imageUrlsText" placeholder="One URL per line"></textarea>
            </label>
            <label class="field">
              <span>Upload image</span>
              <input type="file" accept="image/*" (change)="upload($event)">
            </label>

            <h3>Variants</h3>
            @for (variant of product.variants; track $index; let index = $index) {
              <div class="panel form-grid">
                <div class="two-col">
                  <label class="field">
                    <span>Size</span>
                    <input [name]="'size' + index" [(ngModel)]="variant.size" required>
                  </label>
                  <label class="field">
                    <span>Color</span>
                    <input [name]="'color' + index" [(ngModel)]="variant.color" required>
                  </label>
                </div>
                <div class="two-col">
                  <label class="field">
                    <span>Price</span>
                    <input [name]="'price' + index" [(ngModel)]="variant.price" required type="number" min="1">
                  </label>
                  <label class="field">
                    <span>Stock</span>
                    <input [name]="'stock' + index" [(ngModel)]="variant.stock" required type="number" min="0">
                  </label>
                </div>
                <label class="field">
                  <span>SKU</span>
                  <input [name]="'sku' + index" [(ngModel)]="variant.sku" required>
                </label>
              </div>
            }

            <div class="card-row">
              <button class="secondary-button" type="button" (click)="addVariant()">Add variant</button>
              @if (editingProductId()) {
                <button class="secondary-button" type="button" (click)="cancelProductEdit()">Cancel edit</button>
              }
              <button class="primary-button" type="submit">{{ editingProductId() ? 'Update product' : 'Create product' }}</button>
            </div>
          </form>
        </section>

        <aside class="stack">
          <section class="panel stack">
            <h2>Create category</h2>
            <form class="form-grid" (ngSubmit)="createCategory()">
              <label class="field">
                <span>Name</span>
                <input name="categoryName" [(ngModel)]="category.name" required>
              </label>
              <label class="field">
                <span>Description</span>
                <textarea name="categoryDescription" [(ngModel)]="category.description"></textarea>
              </label>
              <label class="field">
                <span>Image URL</span>
                <input name="categoryImageUrl" [(ngModel)]="category.imageUrl">
              </label>
              <button class="primary-button" type="submit">{{ editingCategoryId() ? 'Update category' : 'Create category' }}</button>
              @if (editingCategoryId()) {
                <button class="secondary-button" type="button" (click)="cancelCategoryEdit()">Cancel edit</button>
              }
            </form>
          </section>

          <section class="panel stack">
            <h2>Categories</h2>
            @for (category of categories(); track category.id) {
              <div class="card-row">
                <div>
                  <h3>{{ category.name }}</h3>
                  <p>{{ category.description }}</p>
                </div>
                <div class="admin-actions">
                  <button class="secondary-button" type="button" (click)="editCategory(category)">Edit</button>
                  <button class="danger-button" type="button" (click)="deleteCategory(category.id)">Delete</button>
                </div>
              </div>
            } @empty {
              <p>No categories yet.</p>
            }
          </section>

          <section class="panel stack">
            <h2>Products</h2>
            @for (item of products(); track item.id) {
              <article class="product-card">
                <div class="card-row">
                  <div>
                    <h3>{{ item.name }}</h3>
                    <p>{{ item.brand }} · {{ item.category }}</p>
                  </div>
                  <span class="badge">{{ item.variants.length }} variants</span>
                </div>
                <div class="card-row">
                  <strong>{{ minPrice(item) | currency }}</strong>
                  <div class="admin-actions">
                    <button class="secondary-button" type="button" (click)="editProduct(item)">Edit</button>
                    <button class="danger-button" type="button" (click)="deleteProduct(item.id)">Delete</button>
                  </div>
                </div>
              </article>
            } @empty {
              <p>No products yet.</p>
            }
          </section>

          @if (message()) {
            <section class="panel">
              <p>{{ message() }}</p>
            </section>
          }
        </aside>
      </div>
    </main>
  `
})
export class AdminComponent implements OnInit {
  readonly categories = signal<Category[]>([]);
  readonly products = signal<Product[]>([]);
  readonly message = signal('');
  readonly editingProductId = signal<string | null>(null);
  readonly editingCategoryId = signal<string | null>(null);
  imageUrlsText = '';
  category = { name: '', description: '', imageUrl: '' };
  product: CreateProductRequest = this.emptyProduct();

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.api.getAdminCategories().subscribe((categories) => this.categories.set(categories));
  }

  loadProducts(): void {
    this.api.getProducts(0, 100).subscribe((products) => this.products.set(products));
  }

  createCategory(): void {
    const request = this.editingCategoryId()
      ? this.api.updateCategory(this.editingCategoryId()!, this.category)
      : this.api.createCategory(this.category);

    request.subscribe({
      next: () => {
        const wasEditing = !!this.editingCategoryId();
        this.category = { name: '', description: '', imageUrl: '' };
        this.editingCategoryId.set(null);
        this.message.set(wasEditing ? 'Category updated.' : 'Category created.');
        this.loadCategories();
      },
      error: () => this.message.set('Category save failed.')
    });
  }

  editCategory(category: Category): void {
    this.editingCategoryId.set(category.id);
    this.category = {
      name: category.name,
      description: category.description || '',
      imageUrl: category.imageUrl || ''
    };
  }

  cancelCategoryEdit(): void {
    this.editingCategoryId.set(null);
    this.category = { name: '', description: '', imageUrl: '' };
  }

  deleteCategory(id: string): void {
    this.api.deleteCategory(id).subscribe({
      next: () => {
        this.message.set('Category deleted.');
        this.loadCategories();
      },
      error: () => this.message.set('Category delete failed. Remove assigned products first.')
    });
  }

  upload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.api.uploadImage(file).subscribe({
      next: (url) => {
        this.imageUrlsText = `${this.imageUrlsText}\n${url}`.trim();
        this.message.set('Image uploaded.');
      },
      error: () => this.message.set('Image upload failed.')
    });
  }

  addVariant(): void {
    this.product.variants = [...this.product.variants, this.emptyVariant()];
  }

  createProduct(): void {
    const payload: CreateProductRequest = {
      ...this.product,
      imageUrls: this.imageUrlsText.split(/\r?\n/).map((url) => url.trim()).filter(Boolean),
      variants: this.product.variants.map((variant) => ({
        ...variant,
        price: Number(variant.price),
        stock: Number(variant.stock)
      }))
    };

    const request = this.editingProductId()
      ? this.api.updateProduct(this.editingProductId()!, payload)
      : this.api.createProduct(payload);

    request.subscribe({
      next: () => {
        const wasEditing = !!this.editingProductId();
        this.product = this.emptyProduct();
        this.imageUrlsText = '';
        this.editingProductId.set(null);
        this.message.set(wasEditing ? 'Product updated.' : 'Product created.');
        this.loadProducts();
      },
      error: () => this.message.set('Product save failed.')
    });
  }

  editProduct(item: Product): void {
    const category = this.categories().find((entry) => entry.name === item.category);
    this.editingProductId.set(item.id);
    this.product = {
      name: item.name,
      description: item.description,
      brand: item.brand,
      categoryId: category?.id || '',
      imageUrls: [...item.images],
      variants: item.variants.map((variant, index) => ({
        size: variant.size,
        color: variant.color,
        price: Number(variant.price),
        stock: variant.stock,
        sku: `${item.id}-${index}-${Date.now()}`.replace(/\s+/g, '-').toLowerCase()
      }))
    };
    this.imageUrlsText = item.images.join('\n');
  }

  cancelProductEdit(): void {
    this.editingProductId.set(null);
    this.product = this.emptyProduct();
    this.imageUrlsText = '';
  }

  deleteProduct(id: string): void {
    this.api.deleteProduct(id).subscribe({
      next: () => {
        this.message.set('Product deleted.');
        this.loadProducts();
      },
      error: () => this.message.set('Product delete failed.')
    });
  }

  minPrice(product: Product): number {
    const prices = product.variants.map((variant) => Number(variant.price)).filter((price) => price > 0);
    return prices.length ? Math.min(...prices) : 0;
  }

  private emptyProduct(): CreateProductRequest {
    return {
      name: '',
      description: '',
      brand: '',
      categoryId: '',
      imageUrls: [],
      variants: [this.emptyVariant()]
    };
  }

  private emptyVariant(): CreateVariantRequest {
    return {
      size: '',
      color: '',
      price: 0,
      stock: 0,
      sku: ''
    };
  }
}

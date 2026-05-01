import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from './environment';
import {
  ApiResponse,
  CartItem,
  Category,
  CheckoutRequest,
  CreateProductRequest,
  NotificationItem,
  Order,
  Payment,
  Product,
  ProductDocument,
  RazorpayConfig,
  RazorpayVerifyRequest
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  getProducts(page = 0, size = 40) {
    return this.http.get<Product[]>(`${this.baseUrl}/products`, {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getProduct(id: string) {
    return this.http.get<Product>(`${this.baseUrl}/products/${id}`);
  }

  filterProducts(filters: { size?: string; color?: string; category?: string }) {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value) {
        params = params.set(key, value);
      }
    });
    return this.http.get<Product[]>(`${this.baseUrl}/products/filter`, { params });
  }

  searchProducts(query: string, page = 0, size = 20) {
    return this.http.get<ProductDocument[]>(`${this.baseUrl}/products/search`, {
      params: new HttpParams().set('q', query).set('page', page).set('size', size)
    });
  }

  getCategories() {
    return this.http.get<Category[]>(`${this.baseUrl}/categories`);
  }

  getAdminCategories() {
    return this.http.get<Category[]>(`${this.baseUrl}/admin/categories`);
  }

  createCategory(category: Pick<Category, 'name' | 'description' | 'imageUrl'>) {
    return this.http.post<Category>(`${this.baseUrl}/admin/categories`, category);
  }

  updateCategory(id: string, category: Pick<Category, 'name' | 'description' | 'imageUrl'>) {
    return this.http.put<Category>(`${this.baseUrl}/admin/categories/${id}`, category);
  }

  deleteCategory(id: string) {
    return this.http.delete(`${this.baseUrl}/admin/categories/${id}`, { responseType: 'text' });
  }

  createProduct(product: CreateProductRequest) {
    return this.http.post(`${this.baseUrl}/products`, product, { responseType: 'text' });
  }

  updateProduct(id: string, product: CreateProductRequest) {
    return this.http.put(`${this.baseUrl}/products/${id}`, product, { responseType: 'text' });
  }

  deleteProduct(id: string) {
    return this.http.delete(`${this.baseUrl}/products/${id}`, { responseType: 'text' });
  }

  uploadImage(file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post(`${this.baseUrl}/admin/images`, form, { responseType: 'text' });
  }

  getCart() {
    return this.http.get<CartItem[]>(`${this.baseUrl}/cart`);
  }

  addToCart(variantId: string, quantity: number) {
    return this.http.post(`${this.baseUrl}/cart/add`, { variantId, quantity }, { responseType: 'text' });
  }

  setCartQuantity(variantId: string, quantity: number) {
    return this.http.put(`${this.baseUrl}/cart/quantity`, { variantId, quantity }, { responseType: 'text' });
  }

  removeCartItem(variantId: string) {
    return this.http.delete(`${this.baseUrl}/cart/${variantId}`, { responseType: 'text' });
  }

  clearCart() {
    return this.http.delete(`${this.baseUrl}/cart/clear-cart`, { responseType: 'text' });
  }

  checkout(request: CheckoutRequest) {
    return this.http.post<ApiResponse<Order>>(`${this.baseUrl}/orders/checkout`, request);
  }

  getOrders() {
    return this.http.get<ApiResponse<Order[]>>(`${this.baseUrl}/orders`);
  }

  getOrder(id: string) {
    return this.http.get<ApiResponse<Order>>(`${this.baseUrl}/orders/${id}`);
  }

  getPayments() {
    return this.http.get<ApiResponse<Payment[]>>(`${this.baseUrl}/payments`);
  }

  getRazorpayConfig() {
    return this.http.get<ApiResponse<RazorpayConfig>>(`${this.baseUrl}/payments/razorpay/config`);
  }

  verifyRazorpayPayment(paymentId: string, request: RazorpayVerifyRequest) {
    return this.http.post<ApiResponse<Payment>>(`${this.baseUrl}/payments/${paymentId}/razorpay/verify`, request);
  }

  simulatePaymentSuccess(paymentId: string) {
    return this.http.post<ApiResponse<Payment>>(`${this.baseUrl}/payments/${paymentId}/simulate-success`, {});
  }

  simulatePaymentFailure(paymentId: string, reason: string) {
    return this.http.post<ApiResponse<Payment>>(`${this.baseUrl}/payments/${paymentId}/simulate-failure`, { reason });
  }

  getNotifications() {
    return this.http.get<ApiResponse<NotificationItem[]>>(`${this.baseUrl}/notifications`);
  }

  markNotificationRead(notificationId: string) {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/notifications/${notificationId}/read`, {});
  }
}

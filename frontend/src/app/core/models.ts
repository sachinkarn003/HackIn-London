export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthTokenResponse {
  token: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Category {
  id: string;
  name: string;
  description?: string;
  imageUrl?: string;
}

export interface ProductVariant {
  id: string;
  size: string;
  color: string;
  price: number;
  stock: number;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  brand: string;
  category: string;
  images: string[];
  variants: ProductVariant[];
}

export interface ProductDocument {
  id: string;
  name: string;
  description: string;
  brand: string;
  category: string;
  imageUrl?: string;
  minPrice?: number;
}

export interface CreateProductRequest {
  name: string;
  description: string;
  brand: string;
  categoryId: string;
  imageUrls: string[];
  variants: CreateVariantRequest[];
}

export interface CreateVariantRequest {
  size: string;
  color: string;
  price: number;
  stock: number;
  sku: string;
}

export interface CartItem {
  variantId: string;
  quantity: number;
  price: number;
  productName: string;
  total: number;
}

export interface CheckoutRequest {
  shippingAddress: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  paymentMethod: string;
}

export interface OrderItem {
  variantId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: string;
  userId: string;
  status: string;
  totalAmount: number;
  shippingAddress: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  paymentMethod: string;
  paymentId?: string;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}

export interface Payment {
  id: string;
  orderId: string;
  userId: string;
  amount: number;
  paymentMethod: string;
  status: string;
  gatewayReference?: string;
  razorpayOrderId?: string;
  razorpayPaymentId?: string;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RazorpayConfig {
  keyId: string;
  currency: string;
  merchantName: string;
}

export interface RazorpayVerifyRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

export interface NotificationItem {
  id: string;
  userId: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth.guard';
import { ShopComponent } from './features/shop.component';
import { ProductDetailComponent } from './features/product-detail.component';
import { LoginComponent } from './features/login.component';
import { CartComponent } from './features/cart.component';
import { OrdersComponent } from './features/orders.component';
import { AdminComponent } from './features/admin.component';
import { AccountComponent } from './features/account.component';
import { CheckoutComponent } from './features/checkout.component';
import { PaymentComponent } from './features/payment.component';
import { OrderDetailComponent } from './features/order-detail.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'shop' },
  { path: 'shop', component: ShopComponent },
  { path: 'products/:id', component: ProductDetailComponent },
  { path: 'login', component: LoginComponent },
  { path: 'cart', component: CartComponent, canActivate: [authGuard] },
  { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard] },
  { path: 'payment/:paymentId', component: PaymentComponent, canActivate: [authGuard] },
  { path: 'orders', component: OrdersComponent, canActivate: [authGuard] },
  { path: 'orders/:id', component: OrderDetailComponent, canActivate: [authGuard] },
  { path: 'account', component: AccountComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [adminGuard] },
  { path: '**', redirectTo: 'shop' }
];

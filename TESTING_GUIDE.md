# Cloth Backend Testing Guide

## Start Everything
```powershell
docker compose build --no-cache
docker compose up -d
docker compose ps
```

Gateway runs on `http://localhost:8085`.

## Admin Setup
Use the bootstrap admin user already configured in compose:
- Email: `admin@cloth.local`
- Password: `Admin@12345`

## Smoke Test Flow
1. Login as admin
```powershell
$adminLogin = Invoke-RestMethod -Method Post -Uri http://localhost:8085/auth/login -ContentType 'application/json' -Body '{"email":"admin@cloth.local","password":"Admin@12345"}'
$adminToken = $adminLogin.data.token
```

2. Create a category
```powershell
$category = Invoke-RestMethod -Method Post -Uri http://localhost:8085/admin/categories -Headers @{ Authorization = "Bearer $adminToken" } -ContentType 'application/json' -Body '{"name":"Men","description":"Menswear","imageUrl":"https://example.com/cat.jpg"}'
```

3. Create a product
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8085/products -Headers @{ Authorization = "Bearer $adminToken" } -ContentType 'application/json' -Body ("{" +
'"name":"Oversized Tee","description":"Heavy cotton tee","brand":"Cloth","categoryId":"' + $category.id + '","imageUrls":["https://example.com/p1.jpg"],"variants":[{"size":"M","color":"Black","price":799,"stock":10,"sku":"TEE-BLK-M"}]}' )
```

4. Signup as customer
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8085/auth/signup -ContentType 'application/json' -Body '{"name":"Test User","email":"test@example.com","password":"password123"}'
```

5. Login as customer
```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8085/auth/login -ContentType 'application/json' -Body '{"email":"test@example.com","password":"password123"}'
$token = $login.data.token
```

6. List products and capture a variant id
```powershell
$products = Invoke-RestMethod -Method Get -Uri http://localhost:8085/products
$variantId = $products[0].variants[0].id
```

7. Add to cart
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8085/cart/add -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' -Body ("{\"variantId\":\"$variantId\",\"quantity\":2}")
```

8. Checkout
```powershell
$order = Invoke-RestMethod -Method Post -Uri http://localhost:8085/orders/checkout -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' -Body '{"shippingAddress":"221B Baker Street","city":"London","state":"Greater London","postalCode":"NW16XE","country":"UK","paymentMethod":"CARD"}'
$orderId = $order.data.id
$paymentId = $order.data.paymentId
```

9. Simulate payment success
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8085/payments/$paymentId/simulate-success -Headers @{ Authorization = "Bearer $token" }
```

10. Verify order confirmation
```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8085/orders/$orderId -Headers @{ Authorization = "Bearer $token" }
```

11. Check notifications
```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8085/notifications -Headers @{ Authorization = "Bearer $token" }
```

## Refund Scenario
1. Checkout another order.
2. Reduce stock to force failure if needed.
3. Simulate payment success.
4. If inventory cannot be reserved, order-service marks the order `REJECTED` and payment-service publishes `REFUNDED`.

## How To Know It Is Working
- `docker compose ps` shows all containers healthy or running.
- `GET http://localhost:8085/products` returns catalog data.
- `POST /orders/checkout` creates an order with `PENDING_PAYMENT` and a `paymentId`.
- `POST /payments/{paymentId}/simulate-success` moves the order through `PAYMENT_COMPLETED` to `CONFIRMED`.
- `GET /notifications` shows order and payment updates.
- `GET /orders/{orderId}` eventually returns `CONFIRMED` if stock exists.
- After `CONFIRMED`, the cart is cleared.
- Product stock reduces after inventory reservation succeeds.

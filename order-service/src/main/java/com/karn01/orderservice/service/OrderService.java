package com.karn01.orderservice.service;

import com.karn01.orderservice.dto.CartItemDto;
import com.karn01.orderservice.dto.CheckoutRequest;
import com.karn01.orderservice.dto.OrderItemResponse;
import com.karn01.orderservice.dto.OrderResponse;
import com.karn01.orderservice.dto.PaymentInitiationRequest;
import com.karn01.orderservice.dto.PaymentResponse;
import com.karn01.orderservice.dto.VariantDetailDto;
import com.karn01.orderservice.entity.Order;
import com.karn01.orderservice.entity.OrderItem;
import com.karn01.orderservice.entity.OrderStatus;
import com.karn01.orderservice.entity.ProcessedEventLog;
import com.karn01.orderservice.event.InventoryReservationCompletedEvent;
import com.karn01.orderservice.event.InventoryReservationRequestedEvent;
import com.karn01.orderservice.event.OrderItemEvent;
import com.karn01.orderservice.event.OrderLifecycleEvent;
import com.karn01.orderservice.event.PaymentStatusEvent;
import com.karn01.orderservice.exception.BadRequestException;
import com.karn01.orderservice.exception.ResourceNotFoundException;
import com.karn01.orderservice.repository.OrderRepository;
import com.karn01.orderservice.repository.ProcessedEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final String PAYMENT_STATUS_EVENT_PREFIX = "PAYMENT_STATUS_";
    private static final String INVENTORY_RESERVATION_COMPLETED_SUCCESS = "INVENTORY_RESERVATION_COMPLETED_SUCCESS";
    private static final String INVENTORY_RESERVATION_COMPLETED_FAILED = "INVENTORY_RESERVATION_COMPLETED_FAILED";

    private final OrderRepository orderRepository;
    private final ProcessedEventLogRepository processedEventLogRepository;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, InventoryReservationRequestedEvent> inventoryKafkaTemplate;
    private final KafkaTemplate<String, OrderLifecycleEvent> orderLifecycleKafkaTemplate;

    @Value("${app.services.cart-service-url}")
    private String cartServiceUrl;

    @Value("${app.services.product-service-url}")
    private String productServiceUrl;

    @Value("${app.services.payment-service-url}")
    private String paymentServiceUrl;

    @Value("${app.kafka.topics.inventory-reservation-requested}")
    private String inventoryReservationRequestedTopic;

    @Value("${app.kafka.topics.order-lifecycle}")
    private String orderLifecycleTopic;

    @Transactional
    public OrderResponse checkout(String userId, CheckoutRequest request) {
        List<CartItemDto> cartItems = getCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Map<UUID, VariantDetailDto> variantDetails = fetchVariantDetails(cartItems).stream()
                .collect(Collectors.toMap(VariantDetailDto::id, Function.identity()));

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setShippingAddress(request.shippingAddress());
        order.setCity(request.city());
        order.setState(request.state());
        order.setPostalCode(request.postalCode());
        order.setCountry(request.country());
        order.setPaymentMethod(request.paymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItemDto cartItem : cartItems) {
            VariantDetailDto variant = variantDetails.get(cartItem.variantId());
            if (variant == null) {
                throw new BadRequestException("Variant not found: " + cartItem.variantId());
            }
            if (cartItem.quantity() == null || cartItem.quantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than zero");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariantId(variant.id());
            orderItem.setProductName(variant.productName());
            orderItem.setQuantity(cartItem.quantity());
            orderItem.setUnitPrice(variant.price());
            orderItem.setSubtotal(variant.price().multiply(BigDecimal.valueOf(cartItem.quantity())));
            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        PaymentResponse paymentResponse = initiatePayment(savedOrder);
        savedOrder.setPaymentId(paymentResponse.id());
        Order persisted = orderRepository.save(savedOrder);

        publishOrderLifecycle(persisted, "Order created and waiting for payment.");
        return toResponse(persisted);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(String userId, UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        return toResponse(order);
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.topics.payment-status}", groupId = "${spring.application.name}", containerFactory = "paymentStatusKafkaListenerContainerFactory")
    public void handlePaymentStatus(PaymentStatusEvent event) {
        String eventType = PAYMENT_STATUS_EVENT_PREFIX + event.status();
        if (processedEventLogRepository.existsByAggregateIdAndEventType(event.orderId(), eventType)) {
            return;
        }

        Order order = orderRepository.findById(event.orderId()).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        switch (event.status()) {
            case "COMPLETED" -> {
                if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED || order.getStatus() == OrderStatus.CONFIRMED) {
                    markEventProcessed(event.orderId(), eventType);
                    return;
                }
                order.setStatus(OrderStatus.PAYMENT_COMPLETED);
                order.setFailureReason(null);
                orderRepository.save(order);
                publishOrderLifecycle(order, "Payment captured successfully.");
                inventoryKafkaTemplate.send(inventoryReservationRequestedTopic, order.getId().toString(), new InventoryReservationRequestedEvent(
                        order.getId(),
                        order.getUserId(),
                        LocalDateTime.now(),
                        order.getItems().stream().map(item -> new OrderItemEvent(item.getVariantId(), item.getProductName(), item.getQuantity(), item.getUnitPrice())).toList()
                ));
                markEventProcessed(event.orderId(), eventType);
            }
            case "FAILED" -> {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                order.setFailureReason(event.reason());
                orderRepository.save(order);
                publishOrderLifecycle(order, "Payment failed: " + event.reason());
                markEventProcessed(event.orderId(), eventType);
            }
            case "REFUNDED" -> {
                order.setStatus(OrderStatus.REFUNDED);
                order.setFailureReason(event.reason());
                orderRepository.save(order);
                publishOrderLifecycle(order, "Payment refunded: " + event.reason());
                markEventProcessed(event.orderId(), eventType);
            }
            default -> {
            }
        }
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.topics.inventory-reservation-completed}", groupId = "${spring.application.name}", containerFactory = "inventoryReservationCompletedKafkaListenerContainerFactory")
    public void handleInventoryReservationCompleted(InventoryReservationCompletedEvent event) {
        String eventType = event.success() ? INVENTORY_RESERVATION_COMPLETED_SUCCESS : INVENTORY_RESERVATION_COMPLETED_FAILED;
        if (processedEventLogRepository.existsByAggregateIdAndEventType(event.orderId(), eventType)) {
            return;
        }

        Order order = orderRepository.findById(event.orderId()).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (event.success()) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setFailureReason(null);
            orderRepository.save(order);
            restTemplate.delete(cartServiceUrl + "/internal/cart/" + order.getUserId());
            publishOrderLifecycle(order, "Order confirmed and inventory reserved.");
            markEventProcessed(event.orderId(), eventType);
            return;
        }

        order.setStatus(OrderStatus.REJECTED);
        order.setFailureReason(event.reason());
        orderRepository.save(order);
        publishOrderLifecycle(order, "Order rejected: " + event.reason());
        restTemplate.postForEntity(paymentServiceUrl + "/internal/refund/order/" + order.getId() + "?reason=Inventory unavailable after payment", null, Void.class);
        markEventProcessed(event.orderId(), eventType);
    }

    private PaymentResponse initiatePayment(Order order) {
        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                paymentServiceUrl + "/internal",
                new PaymentInitiationRequest(order.getId(), order.getUserId(), order.getTotalAmount(), order.getPaymentMethod()),
                PaymentResponse.class
        );
        if (response.getBody() == null) {
            throw new BadRequestException("Payment could not be initiated");
        }
        return response.getBody();
    }
    

    private List<CartItemDto> getCartItems(String userId) {
        ResponseEntity<List<CartItemDto>> response = restTemplate.exchange(
                cartServiceUrl + "/internal/cart/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }

    private List<VariantDetailDto> fetchVariantDetails(List<CartItemDto> cartItems) {
        List<UUID> variantIds = cartItems.stream().map(CartItemDto::variantId).toList();
        ResponseEntity<List<VariantDetailDto>> response = restTemplate.exchange(
                productServiceUrl + "/internal/variants/details",
                HttpMethod.POST,
                new HttpEntity<>(variantIds),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }

    private void publishOrderLifecycle(Order order, String message) {
        orderLifecycleKafkaTemplate.send(orderLifecycleTopic, order.getId().toString(), new OrderLifecycleEvent(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                message,
                order.getTotalAmount(),
                LocalDateTime.now()
        ));
    }

    private void markEventProcessed(UUID aggregateId, String eventType) {
        ProcessedEventLog eventLog = new ProcessedEventLog();
        eventLog.setAggregateId(aggregateId);
        eventLog.setEventType(eventType);
        processedEventLogRepository.save(eventLog);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                order.getCity(),
                order.getState(),
                order.getPostalCode(),
                order.getCountry(),
                order.getPaymentMethod(),
                order.getPaymentId(),
                order.getFailureReason(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(item -> new OrderItemResponse(item.getVariantId(), item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).toList()
        );
    }
}

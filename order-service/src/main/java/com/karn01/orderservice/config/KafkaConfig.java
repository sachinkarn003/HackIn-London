package com.karn01.orderservice.config;

import com.karn01.orderservice.event.InventoryReservationCompletedEvent;
import com.karn01.orderservice.event.PaymentStatusEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Value("${app.kafka.topics.payment-status-dlq}")
    private String paymentStatusDlqTopic;

    @Value("${app.kafka.topics.inventory-reservation-completed-dlq}")
    private String inventoryReservationCompletedDlqTopic;

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (ConsumerRecord<?, ?> record, Exception exception) -> {
                    if (record.topic().equals(paymentStatusDlqTopic) || record.topic().equals(inventoryReservationCompletedDlqTopic)) {
                        return new TopicPartition(record.topic(), record.partition());
                    }
                    if (record.topic().contains("payment.status")) {
                        return new TopicPartition(paymentStatusDlqTopic, record.partition());
                    }
                    return new TopicPartition(inventoryReservationCompletedDlqTopic, record.partition());
                }
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    @Bean
    public ConsumerFactory<String, PaymentStatusEvent> paymentStatusConsumerFactory(org.springframework.boot.autoconfigure.kafka.KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        JsonDeserializer<PaymentStatusEvent> deserializer = new JsonDeserializer<>(PaymentStatusEvent.class);
        return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConsumerFactory<String, InventoryReservationCompletedEvent> inventoryReservationCompletedConsumerFactory(org.springframework.boot.autoconfigure.kafka.KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        JsonDeserializer<InventoryReservationCompletedEvent> deserializer = new JsonDeserializer<>(InventoryReservationCompletedEvent.class);
        return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> paymentStatusKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<String, PaymentStatusEvent> paymentStatusConsumerFactory,
            CommonErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, (ConsumerFactory) paymentStatusConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> inventoryReservationCompletedKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<String, InventoryReservationCompletedEvent> inventoryReservationCompletedConsumerFactory,
            CommonErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, (ConsumerFactory) inventoryReservationCompletedConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}

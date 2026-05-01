package com.karn01.inventoryservice.config;

import com.karn01.inventoryservice.event.InventoryReservationRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
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
    @Value("${app.kafka.topics.inventory-reservation-requested-dlq}")
    private String inventoryReservationRequestedDlqTopic;

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (ConsumerRecord<?, ?> record, Exception exception) -> new TopicPartition(inventoryReservationRequestedDlqTopic, record.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    @Bean
    public ConsumerFactory<String, InventoryReservationRequestedEvent> inventoryReservationRequestedConsumerFactory(org.springframework.boot.autoconfigure.kafka.KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        JsonDeserializer<InventoryReservationRequestedEvent> deserializer = new JsonDeserializer<>(InventoryReservationRequestedEvent.class);
        return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> inventoryReservationRequestedKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<String, InventoryReservationRequestedEvent> inventoryReservationRequestedConsumerFactory,
            CommonErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, (ConsumerFactory) inventoryReservationRequestedConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}

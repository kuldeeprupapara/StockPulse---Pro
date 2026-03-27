package com.stockmarket.query.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.kafka.dto.IndexQuoteKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexQuoteProducer {

	@Value("${kafka.topic.index-quote:stock.quotes.index.v1}")
	private String topic;

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(IndexQuoteKafkaEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(topic, event.getIndexSymbol(), payload);
		} catch (Exception e) {
			log.error("Kafka publish failed for index symbol {}", event.getIndexSymbol(), e);
		}
	}
}

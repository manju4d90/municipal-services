package org.egov.userevent.producer;

import lombok.extern.slf4j.Slf4j;
import org.egov.tracer.kafka.CustomKafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserEventsProducer {

    @Autowired
    private CustomKafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Kafka Producer.
     * 
     * @param topic
     * @param value
     */
    public void push(String topic, Object value) {
        log.info("Topic: "+topic);
        kafkaTemplate.send(topic, value);
    }
}

package com.notification_service.listener;

import com.notification_service.dto.NotificationRequest;
import com.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void listenNotification(NotificationRequest request) {
        logger.info("Received notification event from Kafka for recipient: {}", request.getRecipient());
        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            logger.error("Failed to process Kafka notification", e);
        }
    }
}

package com.notification_service.service;

import com.notification_service.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final List<NotificationRequest> sentNotifications = new ArrayList<>();

    public void sendNotification(NotificationRequest request) {
        logger.info("Sending Notification [Type: {}] to {}:", request.getType(), request.getRecipient());
        logger.info("Subject: {}", request.getSubject());
        logger.info("Body: {}", request.getBody());
        
        sentNotifications.add(request);
    }

    public List<NotificationRequest> getSentNotifications() {
        return sentNotifications;
    }
}

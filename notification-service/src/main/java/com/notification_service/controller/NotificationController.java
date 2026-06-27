package com.notification_service.controller;

import com.notification_service.dto.NotificationRequest;
import com.notification_service.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.ok("Notification sent successfully");
    }

    @GetMapping("/history")
    public ResponseEntity<List<NotificationRequest>> getHistory() {
        return ResponseEntity.ok(notificationService.getSentNotifications());
    }
}

package com.leave_service.dto;

public class NotificationEvent {
    private String recipient;
    private String subject;
    private String body;
    private String type = "EMAIL";

    public NotificationEvent() {}

    public NotificationEvent(String recipient, String subject, String body) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    // Getters and Setters
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

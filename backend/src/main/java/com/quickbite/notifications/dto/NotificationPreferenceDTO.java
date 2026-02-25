package com.quickbite.notifications.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreferenceDTO {
    private Boolean pushEnabled;
    private Boolean emailOrderUpdates;
    private Boolean emailPromotions;
    private Boolean smsDeliveryAlerts;
}

package com.quickbite.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for webhook event from payment provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventDTO {

    private String id;
    
    private String event;
    
    private Object data;
    
    private Long createdAt;
}

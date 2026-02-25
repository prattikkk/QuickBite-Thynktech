package com.quickbite.push.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceRequest {

    @NotBlank(message = "Device token is required")
    private String token;

    private String platform; // WEB, ANDROID, IOS — defaults to WEB
}

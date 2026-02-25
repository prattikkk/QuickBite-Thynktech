package com.quickbite.vendors.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorCreateDTO {

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    private String description;

    private String address;

    private BigDecimal lat;

    private BigDecimal lng;

    private Map<String, String> openHours;

    private BigDecimal deliveryRadiusKm;
}

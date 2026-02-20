package com.quickbite.vendors.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorUpdateDTO {

    private String name;

    private String description;

    private String address;

    private BigDecimal lat;

    private BigDecimal lng;

    private Map<String, String> openHours;

    private Boolean active;
}

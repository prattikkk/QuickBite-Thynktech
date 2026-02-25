package com.quickbite.notifications.service;

import com.quickbite.notifications.dto.NotificationPreferenceDTO;
import com.quickbite.notifications.entity.NotificationPreference;
import com.quickbite.notifications.repository.NotificationPreferenceRepository;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing per-user notification preferences (push, email, SMS toggles).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    /**
     * Get a user's notification preferences, creating defaults if none exist.
     */
    @Transactional
    public NotificationPreferenceDTO getPreferences(UUID userId) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaults(userId));
        return toDTO(pref);
    }

    /**
     * Update a user's notification preferences.
     */
    @Transactional
    public NotificationPreferenceDTO updatePreferences(UUID userId, NotificationPreferenceDTO dto) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaults(userId));

        if (dto.getPushEnabled() != null) {
            pref.setPushEnabled(dto.getPushEnabled());
        }
        if (dto.getEmailOrderUpdates() != null) {
            pref.setEmailOrderUpdates(dto.getEmailOrderUpdates());
        }
        if (dto.getEmailPromotions() != null) {
            pref.setEmailPromotions(dto.getEmailPromotions());
        }
        if (dto.getSmsDeliveryAlerts() != null) {
            pref.setSmsDeliveryAlerts(dto.getSmsDeliveryAlerts());
        }

        NotificationPreference saved = preferenceRepository.save(pref);
        log.info("Updated notification preferences for user {}", userId);
        return toDTO(saved);
    }

    private NotificationPreference createDefaults(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        NotificationPreference pref = NotificationPreference.builder()
                .user(user)
                .pushEnabled(true)
                .emailOrderUpdates(true)
                .emailPromotions(false)
                .smsDeliveryAlerts(true)
                .build();
        return preferenceRepository.save(pref);
    }

    private NotificationPreferenceDTO toDTO(NotificationPreference p) {
        return NotificationPreferenceDTO.builder()
                .pushEnabled(p.getPushEnabled())
                .emailOrderUpdates(p.getEmailOrderUpdates())
                .emailPromotions(p.getEmailPromotions())
                .smsDeliveryAlerts(p.getSmsDeliveryAlerts())
                .build();
    }
}

package com.quickbite.vendors.service;

import com.quickbite.orders.exception.BusinessException;
import com.quickbite.vendors.dto.*;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Modifier;
import com.quickbite.vendors.entity.ModifierGroup;
import com.quickbite.vendors.repository.MenuItemRepository;
import com.quickbite.vendors.repository.ModifierGroupRepository;
import com.quickbite.vendors.repository.ModifierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing modifier groups and individual modifiers for menu items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModifierService {

    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final MenuItemRepository menuItemRepository;

    // ---- Modifier Group operations ----

    /**
     * Get all modifier groups (with nested modifiers) for a menu item.
     *
     * @param menuItemId the menu item UUID
     * @return ordered list of modifier group DTOs
     */
    @Transactional(readOnly = true)
    public List<ModifierGroupDTO> getModifierGroups(UUID menuItemId) {
        List<ModifierGroup> groups = modifierGroupRepository.findByMenuItemIdOrderBySortOrder(menuItemId);
        return groups.stream().map(this::toGroupDTO).collect(Collectors.toList());
    }

    /**
     * Create a new modifier group for a menu item.
     *
     * @param menuItemId the menu item UUID
     * @param dto        creation DTO
     * @return created modifier group DTO
     */
    @Transactional
    public ModifierGroupDTO createModifierGroup(UUID menuItemId, ModifierGroupCreateDTO dto) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new BusinessException("Menu item not found: " + menuItemId));

        int nextSort = modifierGroupRepository.findByMenuItemIdOrderBySortOrder(menuItemId).size();

        ModifierGroup group = ModifierGroup.builder()
                .menuItem(menuItem)
                .name(dto.getName())
                .required(dto.getRequired() != null ? dto.getRequired() : false)
                .minSelections(dto.getMinSelections() != null ? dto.getMinSelections() : 0)
                .maxSelections(dto.getMaxSelections() != null ? dto.getMaxSelections() : 1)
                .sortOrder(nextSort)
                .build();

        group = modifierGroupRepository.save(group);
        log.info("Created modifier group '{}' for menu item {}", group.getName(), menuItemId);
        return toGroupDTO(group);
    }

    /**
     * Update an existing modifier group.
     *
     * @param groupId the modifier group UUID
     * @param dto     update DTO
     * @return updated modifier group DTO
     */
    @Transactional
    public ModifierGroupDTO updateModifierGroup(UUID groupId, ModifierGroupCreateDTO dto) {
        ModifierGroup group = modifierGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Modifier group not found: " + groupId));

        if (dto.getName() != null) group.setName(dto.getName());
        if (dto.getRequired() != null) group.setRequired(dto.getRequired());
        if (dto.getMinSelections() != null) group.setMinSelections(dto.getMinSelections());
        if (dto.getMaxSelections() != null) group.setMaxSelections(dto.getMaxSelections());

        group = modifierGroupRepository.save(group);
        log.info("Updated modifier group {}", groupId);
        return toGroupDTO(group);
    }

    /**
     * Delete a modifier group and all its modifiers.
     *
     * @param groupId the modifier group UUID
     */
    @Transactional
    public void deleteModifierGroup(UUID groupId) {
        if (!modifierGroupRepository.existsById(groupId)) {
            throw new BusinessException("Modifier group not found: " + groupId);
        }
        modifierGroupRepository.deleteById(groupId);
        log.info("Deleted modifier group {}", groupId);
    }

    // ---- Modifier operations ----

    /**
     * Add a modifier to a group.
     *
     * @param groupId the modifier group UUID
     * @param dto     creation DTO
     * @return created modifier DTO
     */
    @Transactional
    public ModifierDTO addModifier(UUID groupId, ModifierCreateDTO dto) {
        ModifierGroup group = modifierGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Modifier group not found: " + groupId));

        int nextSort = modifierRepository.findByGroupIdOrderBySortOrder(groupId).size();

        Modifier modifier = Modifier.builder()
                .group(group)
                .name(dto.getName())
                .priceCents(dto.getPriceCents() != null ? dto.getPriceCents() : 0L)
                .available(dto.getAvailable() != null ? dto.getAvailable() : true)
                .sortOrder(nextSort)
                .build();

        modifier = modifierRepository.save(modifier);
        log.info("Added modifier '{}' to group {}", modifier.getName(), groupId);
        return toModifierDTO(modifier);
    }

    /**
     * Update an existing modifier.
     *
     * @param modifierId the modifier UUID
     * @param dto        update DTO
     * @return updated modifier DTO
     */
    @Transactional
    public ModifierDTO updateModifier(UUID modifierId, ModifierCreateDTO dto) {
        Modifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new BusinessException("Modifier not found: " + modifierId));

        if (dto.getName() != null) modifier.setName(dto.getName());
        if (dto.getPriceCents() != null) modifier.setPriceCents(dto.getPriceCents());
        if (dto.getAvailable() != null) modifier.setAvailable(dto.getAvailable());

        modifier = modifierRepository.save(modifier);
        log.info("Updated modifier {}", modifierId);
        return toModifierDTO(modifier);
    }

    /**
     * Delete a modifier.
     *
     * @param modifierId the modifier UUID
     */
    @Transactional
    public void deleteModifier(UUID modifierId) {
        if (!modifierRepository.existsById(modifierId)) {
            throw new BusinessException("Modifier not found: " + modifierId);
        }
        modifierRepository.deleteById(modifierId);
        log.info("Deleted modifier {}", modifierId);
    }

    // ---- DTO mappers ----

    private ModifierGroupDTO toGroupDTO(ModifierGroup group) {
        return ModifierGroupDTO.builder()
                .id(group.getId())
                .menuItemId(group.getMenuItem().getId())
                .name(group.getName())
                .required(group.getRequired())
                .minSelections(group.getMinSelections())
                .maxSelections(group.getMaxSelections())
                .sortOrder(group.getSortOrder())
                .modifiers(group.getModifiers() != null
                        ? group.getModifiers().stream().map(this::toModifierDTO).collect(Collectors.toList())
                        : List.of())
                .build();
    }

    private ModifierDTO toModifierDTO(Modifier modifier) {
        return ModifierDTO.builder()
                .id(modifier.getId())
                .groupId(modifier.getGroup().getId())
                .name(modifier.getName())
                .priceCents(modifier.getPriceCents())
                .available(modifier.getAvailable())
                .sortOrder(modifier.getSortOrder())
                .build();
    }
}

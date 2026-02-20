package com.quickbite.payments.repository;

import com.quickbite.payments.entity.WebhookDlq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDlqRepository extends JpaRepository<WebhookDlq, UUID> {

    List<WebhookDlq> findByProviderEventId(String providerEventId);

    long count();
}

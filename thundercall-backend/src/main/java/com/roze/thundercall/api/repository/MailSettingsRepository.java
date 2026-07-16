package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.MailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailSettingsRepository extends JpaRepository<MailSettings, Long> {
}

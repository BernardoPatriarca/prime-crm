package com.primecrm.infra.entity.config;

import com.primecrm.infra.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "text")
    private String settingValue;

    @Column(length = 255)
    private String description;
}

package com.primecrm.infra.entity.auth;

import com.primecrm.infra.entity.BaseEntity;
import com.primecrm.infra.entity.domain.DomainValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 180, unique = true)
    private String email;

    @Column(nullable = false, length = 80, unique = true)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_domain_value_id")
    private DomainValue departmentDomainValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_domain_value_id")
    private DomainValue teamDomainValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_domain_value_id")
    private DomainValue positionDomainValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false, length = 10)
    private ThemePreference themePreference = ThemePreference.LIGHT;

    @Column(name = "language_preference", nullable = false, length = 10)
    private String languagePreference = "pt-BR";

    @Column(nullable = false, length = 50)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}

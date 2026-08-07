package com.primecrm.infra.entity.config;

import com.primecrm.infra.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "holidays")
public class Holiday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean national = true;

    @Column(nullable = false)
    private boolean active = true;
}

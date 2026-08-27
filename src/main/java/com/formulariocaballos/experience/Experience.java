package com.formulariocaballos.experience;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "experiences")
public class Experience {

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String level;

    private String duration;

    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer capacity = 5;

    private String image;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "friday_available", nullable = false)
    private Boolean fridayAvailable = false;

    @Column(name = "friday_hours", nullable = false, columnDefinition = "TEXT")
    private String fridayHours = "[]";

    @Column(name = "friday_hour_messages", nullable = false, columnDefinition = "TEXT")
    private String fridayHourMessages = "{}";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String hours = "[]";

    @Column(name = "hour_messages", nullable = false, columnDefinition = "TEXT")
    private String hourMessages = "{}";
}

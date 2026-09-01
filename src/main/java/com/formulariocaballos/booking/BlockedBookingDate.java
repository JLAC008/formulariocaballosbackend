package com.formulariocaballos.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "blocked_booking_dates")
public class BlockedBookingDate {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "date_key")
    private LocalDate dateKey;

    public BlockedBookingDate(LocalDate dateKey) {
        this.dateKey = dateKey;
    }
}

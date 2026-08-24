package com.formulariocaballos.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BonusPackRepository extends JpaRepository<BonusPack, Long> {
    List<BonusPack> findByActiveTrueOrderByBonusesAscPriceCentsAsc();
    List<BonusPack> findAllByOrderByActiveDescBonusesAscPriceCentsAsc();
    Optional<BonusPack> findFirstByBonusesAndActiveTrueOrderByPriceCentsAsc(Integer bonuses);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

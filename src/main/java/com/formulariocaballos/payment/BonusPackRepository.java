package com.formulariocaballos.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BonusPackRepository extends JpaRepository<BonusPack, Long> {
    List<BonusPack> findByActiveTrueAndDeletedFalseOrderByBonusesAscPriceCentsAsc();
    List<BonusPack> findByDeletedFalseOrderByActiveDescBonusesAscPriceCentsAsc();
    Optional<BonusPack> findFirstByBonusesAndActiveTrueAndDeletedFalseOrderByPriceCentsAsc(Integer bonuses);
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);
}

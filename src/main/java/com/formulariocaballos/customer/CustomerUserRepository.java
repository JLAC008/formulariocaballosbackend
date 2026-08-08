package com.formulariocaballos.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long> {
    Optional<CustomerUser> findByEmailIgnoreCase(String email);
}

package com.example.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data generates the implementation at runtime: save, findById, findAll, etc.
public interface AccountRepository extends JpaRepository<Account, Long> {
}

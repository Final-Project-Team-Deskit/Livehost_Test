package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Sanction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {
}

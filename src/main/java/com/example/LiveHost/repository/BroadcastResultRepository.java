package com.example.LiveHost.repository;

import com.example.LiveHost.entity.BroadcastResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastResultRepository extends JpaRepository<BroadcastResult, Long>, BroadcastResultRepositoryCustom {
}

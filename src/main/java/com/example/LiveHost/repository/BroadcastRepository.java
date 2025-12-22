package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
}

package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.Vod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VodRepository extends JpaRepository<Vod, Long> {
    Optional<Vod> findByBroadcast(Broadcast broadcast);
}

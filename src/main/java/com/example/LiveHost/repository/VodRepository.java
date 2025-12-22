package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Vod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VodRepository extends JpaRepository<Vod, Long> {
}

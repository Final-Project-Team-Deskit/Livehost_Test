package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.BroadcastProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastProductRepository extends JpaRepository<BroadcastProduct, Long> {
    void deleteByBroadcast(Broadcast broadcast);
}

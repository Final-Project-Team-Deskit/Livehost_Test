package com.example.LiveHost.others.repository;

import com.example.LiveHost.others.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Seller findByLoginId(String loginId);
}

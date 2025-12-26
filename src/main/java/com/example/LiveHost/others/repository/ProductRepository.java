package com.example.LiveHost.others.repository;

import com.example.LiveHost.others.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 1. 내 상품 목록 조회
    @Query("SELECT p FROM Product p WHERE p.seller.sellerId = :sellerId AND p.status IN ('ON_SALE', 'READY', 'LIMITED_SALE')")
    List<Product> findAllAvailableBySellerId(@Param("sellerId") Long sellerId);

    // 2. 검색 조회 (상품명 포함 검색)
    @Query("SELECT p FROM Product p WHERE p.seller.sellerId = :sellerId AND p.status IN ('ON_SALE', 'READY', 'LIMITED_SALE') AND p.productName LIKE %:keyword%")
    List<Product> searchAvailableBySellerId(@Param("sellerId") Long sellerId, @Param("keyword") String keyword);
}

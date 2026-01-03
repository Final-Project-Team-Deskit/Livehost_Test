package com.example.LiveHost.others.repository;

import com.example.LiveHost.others.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Member findByLoginId(String loginId);
}

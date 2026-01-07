package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.Sanction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SanctionRepository extends JpaRepository<Sanction, Long>, SanctionRepositoryCustom {
    Integer countByBroadcast(Broadcast broadcast);

    int countByBroadcastAndStatusIn(Broadcast broadcast, Collection<SanctionType> statuses);

    @EntityGraph(attributePaths = "member")
    List<Sanction> findByBroadcastOrderByCreatedAtDesc(Broadcast broadcast);
}

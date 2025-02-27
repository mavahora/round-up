package com.starling.roundup.repository;

import com.starling.roundup.entity.RoundUpRequest;
import com.starling.roundup.entity.Status;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RoundUpRequestRepository extends JpaRepository<RoundUpRequest, String> {
    Optional<RoundUpRequest> findByAccountIdAndWeekCommencing(String accountUid, LocalDate weekCommencing);

    boolean existsByAccountIdAndWeekCommencingAndStatus(String accountId, LocalDate weekCommencing, Status status);

    @Modifying
    @Transactional
    @Query("UPDATE RoundUpRequest r SET r.status = :status, r.roundUpAmount = :amount WHERE r.accountId = :accountId AND r.weekCommencing = :weekCommencing")
    void updateStatusAndAmountByAccountAndWeek(String accountId, LocalDate weekCommencing, Status status, long amount);
}

package com.morph.wallet.repository;


import com.morph.wallet.entity.IdempotencyKey;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository 
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, Long> {
    @Query("SELECT idk FROM IdempotencyKey idk WHERE idk.idempotencyKey = :key")
    Optional<IdempotencyKey> findByKey(@Param("key") String key);
}
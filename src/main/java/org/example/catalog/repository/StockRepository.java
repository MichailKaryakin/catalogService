package org.example.catalog.repository;

import org.example.catalog.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);

    @Modifying
    @Query("""
            UPDATE Stock s
            SET s.quantity = s.quantity - :qty,
                s.reserved = s.reserved + :qty
            WHERE s.product.id = :productId
              AND s.quantity >= :qty
            """)
    int reserveStock(@Param("productId") UUID productId, @Param("qty") int qty);

    @Modifying
    @Query("""
            UPDATE Stock s
            SET s.quantity = s.quantity + :qty,
                s.reserved = s.reserved - :qty
            WHERE s.product.id = :productId
              AND s.reserved >= :qty
            """)
    int releaseStock(@Param("productId") UUID productId, @Param("qty") int qty);
}

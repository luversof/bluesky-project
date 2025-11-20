package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Table("StockPriceHistory")
public class StockPriceHistory {

    @Id
    @Column("id")
    private UUID id;

    @NotNull
    @Column("stockItem_id")
    private UUID stockItemId;

    @NotNull
    @Column("price")
    private BigDecimal price;

    /**
     * 가격이 적용된 날짜/시각 (보통 일별 종가 기준으로 사용)
     */
    @NotNull
    @Column("priceDate")
    private Instant priceDate;
}

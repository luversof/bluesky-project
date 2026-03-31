package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("StockPriceHistory")
public class StockPriceHistory {

    @Id
    @Column("id")
    private UUID id;

    @Column("stockItem_id")
    private UUID stockItemId;

    @Column("tradeDate")
    private LocalDate tradeDate;

    @Column("openPrice")
    private BigDecimal openPrice;

    @Column("highPrice")
    private BigDecimal highPrice;

    @Column("lowPrice")
    private BigDecimal lowPrice;

    @Column("closePrice")
    private BigDecimal closePrice;

    @Column("volume")
    private long volume;

    @LastModifiedDate
    @Column("updatedDate")
    private Instant updatedDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStockItemId() {
        return stockItemId;
    }

    public void setStockItemId(UUID stockItemId) {
        this.stockItemId = stockItemId;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }
}

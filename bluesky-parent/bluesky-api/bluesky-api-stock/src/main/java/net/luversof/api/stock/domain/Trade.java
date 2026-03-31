package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.luversof.api.stock.constant.TradeType;

/** 주식 매매 내역 */
@Table("Trade")
public class Trade {

    @Id
    @Column("id")
    private UUID id;

    @Column("account_id")
    private UUID accountId;

    @Column("stockItem_id")
    private UUID stockItemId;

    @Column("type")
    private TradeType type;

    @Column("quantity")
    private int quantity;

    @Column("price")
    private BigDecimal price;

    @Column("fee")
    private BigDecimal fee;

    @Column("tax")
    private BigDecimal tax;

    @Column("tradeDate")
    private Instant tradeDate;

    @Column("realizedProfit")
    private BigDecimal realizedProfit;

    @Column("exchangeRate")
    private BigDecimal exchangeRate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getStockItemId() {
        return stockItemId;
    }

    public void setStockItemId(UUID stockItemId) {
        this.stockItemId = stockItemId;
    }

    public TradeType getType() {
        return type;
    }

    public void setType(TradeType type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public Instant getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(Instant tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getRealizedProfit() {
        return realizedProfit;
    }

    public void setRealizedProfit(BigDecimal realizedProfit) {
        this.realizedProfit = realizedProfit;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    @Override
    public String toString() {
        return "Trade [id="
                + id
                + ", accountId="
                + accountId
                + ", stockItemId="
                + stockItemId
                + ", type="
                + type
                + ", quantity="
                + quantity
                + ", price="
                + price
                + ", fee="
                + fee
                + ", tax="
                + tax
                + ", tradeDate="
                + tradeDate
                + ", realizedProfit="
                + realizedProfit
                + "]";
    }
}

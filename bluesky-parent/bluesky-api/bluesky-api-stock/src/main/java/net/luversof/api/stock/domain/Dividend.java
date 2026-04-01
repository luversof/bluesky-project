package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 주식 배당 내역 */
@Table("Dividend")
public class Dividend {

    @Id
    @Column("id")
    private UUID id;

    @Column("account_id")
    private UUID accountId;

    @Column("stockItem_id")
    private UUID stockItemId;

    @Transient private String stockItemName;

    @Column("type")
    private String type;

    @Column("quantity")
    private Integer quantity;

    /** 주당 배당금 (예: 100원) */
    @Column("amountPerShare")
    private BigDecimal amountPerShare;

    /** 주당 과세금 (예: 15.4원) */
    @Column("taxPerShare")
    private BigDecimal taxPerShare;

    /** 세전 배당금 총액 ( = quantity * amountPerShare ) */
    @Column("grossAmount")
    private BigDecimal grossAmount;

    /** 세금 총액 */
    @Column("tax")
    private BigDecimal tax;

    /** 기타 수수료 */
    @Column("fee")
    private BigDecimal fee;

    /** 배당기준일 */
    @Column("recordDate")
    private Instant recordDate;

    /** 배당지급일 */
    @Column("payDate")
    private Instant payDate;

    /** 세후 배당금 (계산 필드: 세전금액 - 세금 - 수수료) */
    public BigDecimal getNetAmount() {
        BigDecimal g = grossAmount != null ? grossAmount : BigDecimal.ZERO;
        BigDecimal t = tax != null ? tax : BigDecimal.ZERO;
        BigDecimal f = fee != null ? fee : BigDecimal.ZERO;
        return g.subtract(t).subtract(f);
    }

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

    public String getStockItemName() {
        return stockItemName;
    }

    public void setStockItemName(String stockItemName) {
        this.stockItemName = stockItemName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmountPerShare() {
        return amountPerShare;
    }

    public void setAmountPerShare(BigDecimal amountPerShare) {
        this.amountPerShare = amountPerShare;
    }

    public BigDecimal getTaxPerShare() {
        return taxPerShare;
    }

    public void setTaxPerShare(BigDecimal taxPerShare) {
        this.taxPerShare = taxPerShare;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
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

    public Instant getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Instant recordDate) {
        this.recordDate = recordDate;
    }

    public Instant getPayDate() {
        return payDate;
    }

    public void setPayDate(Instant payDate) {
        this.payDate = payDate;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Dividend other = (Dividend) obj;
        return Objects.equals(accountId, other.accountId)
                && Objects.equals(fee, other.fee)
                && Objects.equals(id, other.id)
                && Objects.equals(payDate, other.payDate)
                && Objects.equals(amountPerShare, other.amountPerShare)
                && Objects.equals(taxPerShare, other.taxPerShare)
                && Objects.equals(grossAmount, other.grossAmount)
                && Objects.equals(quantity, other.quantity)
                && Objects.equals(recordDate, other.recordDate)
                && Objects.equals(stockItemId, other.stockItemId)
                && Objects.equals(stockItemName, other.stockItemName)
                && Objects.equals(tax, other.tax)
                && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                accountId,
                fee,
                id,
                payDate,
                amountPerShare,
                taxPerShare,
                grossAmount,
                quantity,
                recordDate,
                stockItemId,
                stockItemName,
                tax,
                type);
    }

    @Override
    public String toString() {
        return "Dividend [id="
                + id
                + ", accountId="
                + accountId
                + ", stockItemId="
                + stockItemId
                + ", stockItemName="
                + stockItemName
                + ", type="
                + type
                + ", quantity="
                + quantity
                + ", amountPerShare="
                + amountPerShare
                + ", taxPerShare="
                + taxPerShare
                + ", grossAmount="
                + grossAmount
                + ", fee="
                + fee
                + ", tax="
                + tax
                + ", recordDate="
                + recordDate
                + ", payDate="
                + payDate
                + "]";
    }
}

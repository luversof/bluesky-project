package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("MonthlyDividendPayout")
public class MonthlyDividendPayout {

  @Id
  @Column("id")
  private UUID id;

  @Column("stockItem_id")
  private UUID stockItemId;

  @Column("recordDate")
  private LocalDate recordDate;

  @Column("payDate")
  private LocalDate payDate;

  /**
   * 분배율(%). <b>어떤 가져오기 경로도 이 값을 채우지 않는다.</b>
   *
   * <p>실측 2026-08-23: 저장된 지급 이력 202 건이 모두 null 이다. 네 곳의 출처 파서(KODEX/PLUS/RISE/TIGER)가 만드는 일괄 입력은
   * "지급기준일 / 실지급일 / 분배금액 / 주당과세표준액" 4 칸뿐이고 분배율 칸을 만들지 않는다. 일괄 입력 파서는 헤더에 분배율이 있으면 읽도록 돼 있어서 <b>사람이
   * 직접 넣을 때만</b> 값이 들어온다.
   *
   * <p>화면에는 자리가 있다 &mdash; 월배당 참조 화면의 입력 칸과 표 컬럼(값이 없으면 {@code -}). 즉 지금은 202 행 모두 그 컬럼이 비어 있다.
   *
   * <p>계산에는 쓰이지 않는다. 예상 배당·예상 과세표준은 주당 분배금과 주당 과세표준액만 쓴다. 그래서 비어 있어도 숫자가 틀어지지는 않는다.
   */
  @Column("distributionRatePct")
  private BigDecimal distributionRatePct;

  @Column("dividendAmountPerShare")
  private BigDecimal dividendAmountPerShare;

  @Column("taxableBasePerShare")
  private BigDecimal taxableBasePerShare;

  @Column("createdDate")
  private Instant createdDate;

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

  public LocalDate getRecordDate() {
    return recordDate;
  }

  public void setRecordDate(LocalDate recordDate) {
    this.recordDate = recordDate;
  }

  public LocalDate getPayDate() {
    return payDate;
  }

  public void setPayDate(LocalDate payDate) {
    this.payDate = payDate;
  }

  public BigDecimal getDistributionRatePct() {
    return distributionRatePct;
  }

  public void setDistributionRatePct(BigDecimal distributionRatePct) {
    this.distributionRatePct = distributionRatePct;
  }

  public BigDecimal getDividendAmountPerShare() {
    return dividendAmountPerShare;
  }

  public void setDividendAmountPerShare(BigDecimal dividendAmountPerShare) {
    this.dividendAmountPerShare = dividendAmountPerShare;
  }

  public BigDecimal getTaxableBasePerShare() {
    return taxableBasePerShare;
  }

  public void setTaxableBasePerShare(BigDecimal taxableBasePerShare) {
    this.taxableBasePerShare = taxableBasePerShare;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public Instant getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Instant updatedDate) {
    this.updatedDate = updatedDate;
  }
}

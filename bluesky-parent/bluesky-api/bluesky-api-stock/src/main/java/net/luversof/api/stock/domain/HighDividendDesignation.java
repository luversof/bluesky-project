package net.luversof.api.stock.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 고배당기업 지정(조세특례제한법 제104조의27). 사용자 구분 없는 공용 값이다 - 회사가 사업연도마다 공시한다(KRX KIND "고배당기업 공시내역").
 *
 * <p>판정은 지급일 기간으로 한다. 12월 결산법인의 결산배당은 다음 해 4월쯤 지급되므로 지급 연도(1~12월)로 자르면 사업연도와 어긋나고, 원장의 기준일은 지급일과 같은
 * 값이라 쓸 수 없다. {@code businessYear} 는 표시용이다.
 */
@Table("HighDividendDesignation")
public class HighDividendDesignation {

  @Id
  @Column("id")
  private UUID id;

  @Column("stockItem_id")
  private UUID stockItemId;

  @Column("businessYear")
  private int businessYear;

  @Column("payDateFrom")
  private LocalDate payDateFrom;

  @Column("payDateTo")
  private LocalDate payDateTo;

  @Column("note")
  private String note;

  /** 이 지정이 그 종목의 그 지급일 배당을 덮는지. 기간은 시작일 · 끝일을 모두 포함한다. */
  public boolean covers(UUID stockItemId, LocalDate payDate) {
    return stockItemId != null
        && payDate != null
        && stockItemId.equals(this.stockItemId)
        && !payDate.isBefore(payDateFrom)
        && !payDate.isAfter(payDateTo);
  }

  /** 지정 목록 중 하나라도 그 배당을 덮으면 고배당 배당이다. */
  public static boolean isHighDividend(
      Collection<HighDividendDesignation> designations, UUID stockItemId, LocalDate payDate) {
    return designations != null
        && designations.stream().anyMatch(d -> d.covers(stockItemId, payDate));
  }

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

  public int getBusinessYear() {
    return businessYear;
  }

  public void setBusinessYear(int businessYear) {
    this.businessYear = businessYear;
  }

  public LocalDate getPayDateFrom() {
    return payDateFrom;
  }

  public void setPayDateFrom(LocalDate payDateFrom) {
    this.payDateFrom = payDateFrom;
  }

  public LocalDate getPayDateTo() {
    return payDateTo;
  }

  public void setPayDateTo(LocalDate payDateTo) {
    this.payDateTo = payDateTo;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}

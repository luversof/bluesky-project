package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** {@link StockPrediction} 하나의 채점 결과. 만기(기준일 + horizonDays 거래일) 종가가 들어온 뒤 한 번만 만든다. */
@Table("StockPredictionGrade")
public class StockPredictionGrade {

  @Id
  @Column("id")
  private UUID id;

  @Column("prediction_id")
  private UUID predictionId;

  @Column("targetDate")
  private LocalDate targetDate;

  @Column("actualClose")
  private BigDecimal actualClose;

  @Column("actualChangePct")
  private BigDecimal actualChangePct;

  @Column("actualDirection")
  private String actualDirection;

  @Column("hit")
  private boolean hit;

  @Column("late")
  private boolean late;

  @Column("alwaysUpHit")
  private boolean alwaysUpHit;

  /** 기준일 앞 종가가 없으면 null. */
  @Column("prevDirectionHit")
  private Boolean prevDirectionHit;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getPredictionId() {
    return predictionId;
  }

  public void setPredictionId(UUID predictionId) {
    this.predictionId = predictionId;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(LocalDate targetDate) {
    this.targetDate = targetDate;
  }

  public BigDecimal getActualClose() {
    return actualClose;
  }

  public void setActualClose(BigDecimal actualClose) {
    this.actualClose = actualClose;
  }

  public BigDecimal getActualChangePct() {
    return actualChangePct;
  }

  public void setActualChangePct(BigDecimal actualChangePct) {
    this.actualChangePct = actualChangePct;
  }

  public String getActualDirection() {
    return actualDirection;
  }

  public void setActualDirection(String actualDirection) {
    this.actualDirection = actualDirection;
  }

  public boolean isHit() {
    return hit;
  }

  public void setHit(boolean hit) {
    this.hit = hit;
  }

  public boolean isLate() {
    return late;
  }

  public void setLate(boolean late) {
    this.late = late;
  }

  public boolean isAlwaysUpHit() {
    return alwaysUpHit;
  }

  public void setAlwaysUpHit(boolean alwaysUpHit) {
    this.alwaysUpHit = alwaysUpHit;
  }

  public Boolean getPrevDirectionHit() {
    return prevDirectionHit;
  }

  public void setPrevDirectionHit(Boolean prevDirectionHit) {
    this.prevDirectionHit = prevDirectionHit;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }
}

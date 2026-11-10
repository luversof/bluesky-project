package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 종목 방향 예측. 넣기만 하고 고치지 않는다 - 채점은 {@link StockPredictionGrade} 에 따로 쌓는다(사후에 고칠 수 있으면 채점을 믿을 수 없다).
 */
@Table("StockPrediction")
public class StockPrediction {

  @Id
  @Column("id")
  private UUID id;

  /** 분석 회차: 한 번의 제출로 저장한 예측 묶음. 회차 기능 전(2026-11-10) 예측은 제출 시각으로 채웠다. */
  @Column("runId")
  private UUID runId;

  @Column("stockItem_id")
  private UUID stockItemId;

  @Column("baseDate")
  private LocalDate baseDate;

  @Column("basePrice")
  private BigDecimal basePrice;

  @Column("horizonDays")
  private int horizonDays;

  @Column("direction")
  private String direction;

  @Column("confidence")
  private int confidence;

  @Column("rationale")
  private String rationale;

  @Column("modelId")
  private String modelId;

  @Column("promptVersion")
  private String promptVersion;

  @Column("inputSnapshot")
  private Map<String, Object> inputSnapshot;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  public UUID getId() {
    return id;
  }

  public UUID getRunId() {
    return runId;
  }

  public void setRunId(UUID runId) {
    this.runId = runId;
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

  public LocalDate getBaseDate() {
    return baseDate;
  }

  public void setBaseDate(LocalDate baseDate) {
    this.baseDate = baseDate;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
  }

  public int getHorizonDays() {
    return horizonDays;
  }

  public void setHorizonDays(int horizonDays) {
    this.horizonDays = horizonDays;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public int getConfidence() {
    return confidence;
  }

  public void setConfidence(int confidence) {
    this.confidence = confidence;
  }

  public String getRationale() {
    return rationale;
  }

  public void setRationale(String rationale) {
    this.rationale = rationale;
  }

  public String getModelId() {
    return modelId;
  }

  public void setModelId(String modelId) {
    this.modelId = modelId;
  }

  public String getPromptVersion() {
    return promptVersion;
  }

  public void setPromptVersion(String promptVersion) {
    this.promptVersion = promptVersion;
  }

  public Map<String, Object> getInputSnapshot() {
    return inputSnapshot;
  }

  public void setInputSnapshot(Map<String, Object> inputSnapshot) {
    this.inputSnapshot = inputSnapshot;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }
}

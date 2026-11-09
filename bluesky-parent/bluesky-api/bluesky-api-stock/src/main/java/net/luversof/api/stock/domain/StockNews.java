package net.luversof.api.stock.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 시황 분석 입력으로 모은 종목 뉴스(NAVER) · 공시(DART). */
@Table("StockNews")
public class StockNews {

  public static final String SOURCE_NAVER = "NAVER";
  public static final String SOURCE_DART = "DART";

  @Id
  @Column("id")
  private UUID id;

  @Column("stockItem_id")
  private UUID stockItemId;

  @Column("source")
  private String source;

  @Column("externalId")
  private String externalId;

  @Column("title")
  private String title;

  @Column("summary")
  private String summary;

  @Column("url")
  private String url;

  @Column("publishedDate")
  private Instant publishedDate;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

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

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Instant getPublishedDate() {
    return publishedDate;
  }

  public void setPublishedDate(Instant publishedDate) {
    this.publishedDate = publishedDate;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }
}

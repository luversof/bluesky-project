package net.luversof.web.dynamiccrud2.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 프로젝트(메뉴 계층 최상위) */
@Table("Project")
public class Project {

  @Id
  @Column("id")
  private UUID id;

  /** 주소에 쓰는 이름(영문 소문자 · 숫자 · -). 같은 부모 안에서 유일 */
  @Column("code")
  private String code;

  @Column("name")
  private String name;

  @Column("displayOrder")
  private int displayOrder;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }
}

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

  /** 끄면 사용 화면 메뉴 목록에서 이 프로젝트(와 그 아래 전부)를 뺀다. 주소로는 그대로 열린다 */
  @Column("enableDisplay")
  private boolean enableDisplay = true;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  public boolean isEnableDisplay() {
    return enableDisplay;
  }

  public void setEnableDisplay(boolean enableDisplay) {
    this.enableDisplay = enableDisplay;
  }

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

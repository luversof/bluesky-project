package net.luversof.web.dynamiccrud2.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 메인 메뉴(프로젝트 아래) */
@Table("MainMenu")
public class MainMenu {

  @Id
  @Column("id")
  private UUID id;

  @Column("project_id")
  private UUID projectId;

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

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
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

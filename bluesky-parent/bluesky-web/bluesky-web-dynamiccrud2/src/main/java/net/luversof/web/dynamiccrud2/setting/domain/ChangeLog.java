package net.luversof.web.dynamiccrud2.setting.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 입력 · 수정 · 삭제 실행 이력 */
@Table("ChangeLog")
public class ChangeLog {

  @Id
  @Column("id")
  private UUID id;

  @Column("menu_id")
  private UUID menuId;

  @Column("menuQuery_id")
  private UUID menuQueryId;

  @Column("username")
  private String username;

  @Column("bindValues")
  private Map<String, Object> bindValues;

  @Column("affectedRows")
  private int affectedRows;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getMenuId() {
    return menuId;
  }

  public void setMenuId(UUID menuId) {
    this.menuId = menuId;
  }

  public UUID getMenuQueryId() {
    return menuQueryId;
  }

  public void setMenuQueryId(UUID menuQueryId) {
    this.menuQueryId = menuQueryId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Map<String, Object> getBindValues() {
    return bindValues;
  }

  public void setBindValues(Map<String, Object> bindValues) {
    this.bindValues = bindValues;
  }

  public int getAffectedRows() {
    return affectedRows;
  }

  public void setAffectedRows(int affectedRows) {
    this.affectedRows = affectedRows;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }
}

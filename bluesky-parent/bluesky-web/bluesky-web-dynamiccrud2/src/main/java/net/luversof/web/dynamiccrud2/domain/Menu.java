package net.luversof.web.dynamiccrud2.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** 메뉴. 기준 테이블 하나 + 작업(READ 1개, INSERT · UPDATE · DELETE N 개). */
@Table("Menu")
public class Menu {

  @Id
  @Column("id")
  private UUID id;

  @Column("mainMenu_id")
  private UUID mainMenuId;

  /** 주소에 쓰는 이름(영문 소문자 · 숫자 · -). 같은 부모 안에서 유일 */
  @Column("code")
  private String code;

  /** 메뉴 등록 때 고른 기준 테이블 */
  @Column("dbTable_id")
  private UUID dbTableId;

  @Column("name")
  private String name;

  /** 역할 문자열 하나(예: ROLE_ADMIN). null = 로그인한 사용자 누구나 */
  @Column("authority")
  private String authority;

  @Column("pageSize")
  private int pageSize = 20;

  @Column("enableExcel")
  private boolean enableExcel;

  @Column("enableDisplay")
  private boolean enableDisplay = true;

  @Column("displayOrder")
  private int displayOrder;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  @MappedCollection(idColumn = "menu_id")
  private Set<MenuQuery> queries = new LinkedHashSet<>();

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

  public UUID getMainMenuId() {
    return mainMenuId;
  }

  public void setMainMenuId(UUID mainMenuId) {
    this.mainMenuId = mainMenuId;
  }

  public UUID getDbTableId() {
    return dbTableId;
  }

  public void setDbTableId(UUID dbTableId) {
    this.dbTableId = dbTableId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAuthority() {
    return authority;
  }

  public void setAuthority(String authority) {
    this.authority = authority;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public boolean isEnableExcel() {
    return enableExcel;
  }

  public void setEnableExcel(boolean enableExcel) {
    this.enableExcel = enableExcel;
  }

  public boolean isEnableDisplay() {
    return enableDisplay;
  }

  public void setEnableDisplay(boolean enableDisplay) {
    this.enableDisplay = enableDisplay;
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

  public Set<MenuQuery> getQueries() {
    return queries;
  }

  public void setQueries(Set<MenuQuery> queries) {
    this.queries = queries;
  }
}

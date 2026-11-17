package net.luversof.web.dynamiccrud2.domain;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 메뉴의 작업 하나(Menu 묶음의 하위). 쿼리 원문 + 파라미터별 설정. */
@Table("MenuQuery")
public class MenuQuery {

  /** ChangeLog 가 가리키므로 하위 엔티티지만 id 를 둔다. 비어 있으면 저장 전 콜백이 채운다. */
  @Id
  @Column("id")
  private UUID id;

  @Column("kind")
  private QueryKind kind;

  @Column("name")
  private String name;

  /** :name 파라미터를 쓴 쿼리 원문(저장 시 고치지 않는다) */
  @Column("queryText")
  private String queryText;

  /** 파라미터 이름별 설정(역할 · 필수 · 입력 형식 · 기본값) */
  @Column("paramSettings")
  private Map<String, Object> paramSettings;

  /** 작업 실행 권한. null = 메뉴 권한을 따른다 */
  @Column("authority")
  private String authority;

  @Column("displayOrder")
  private int displayOrder;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public QueryKind getKind() {
    return kind;
  }

  public void setKind(QueryKind kind) {
    this.kind = kind;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getQueryText() {
    return queryText;
  }

  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  public Map<String, Object> getParamSettings() {
    return paramSettings;
  }

  public void setParamSettings(Map<String, Object> paramSettings) {
    this.paramSettings = paramSettings;
  }

  public String getAuthority() {
    return authority;
  }

  public void setAuthority(String authority) {
    this.authority = authority;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}

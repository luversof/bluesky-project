package net.luversof.web.dynamiccrud2.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 설정 변경 이력(관리 화면에서 바꾼 프로젝트 · 메인 메뉴 · 메뉴 · 쿼리 · 검색 · 필드 설정). 사용 화면의 데이터 변경(ChangeLog)과 따로 둔다.
 * changes = {항목: {"before": 이전, "after": 이후}} - 바뀐 항목만(만들기는 after 만, 지우기는 before 만).
 */
@Table("SettingLog")
public class SettingLog {

  /** 무엇을 바꿨나 */
  public enum Target {
    PROJECT,
    MAIN_MENU,
    MENU,
    QUERY,
    SEARCH,
    FIELD
  }

  /** 어떻게 바꿨나 */
  public enum Action {
    CREATE,
    UPDATE,
    DELETE,
    REORDER
  }

  @Id
  @Column("id")
  private UUID id;

  /** 걸러 볼 때 쓰는 프로젝트(필드는 테이블 단위라 여러 프로젝트가 공유 - 비워 둔다) */
  @Column("project_id")
  private UUID projectId;

  /** 걸러 볼 때 쓰는 메뉴(메뉴 · 쿼리 · 검색). 프로젝트 · 메인 메뉴 · 필드는 비어 있다 */
  @Column("menu_id")
  private UUID menuId;

  @Column("target")
  private Target target;

  /** 바꾼 대상의 id(필드는 DbTable id, 순서 바꾸기는 비어 있을 수 있다) */
  @Column("targetId")
  private UUID targetId;

  /** 바꾼 대상의 이름(지운 뒤에도 무엇이었는지 알도록, 바꾼 때의 이름) */
  @Column("targetName")
  private String targetName;

  @Column("action")
  private Action action;

  @Column("username")
  private String username;

  @Column("changes")
  private Map<String, Object> changes;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

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

  public UUID getMenuId() {
    return menuId;
  }

  public void setMenuId(UUID menuId) {
    this.menuId = menuId;
  }

  public Target getTarget() {
    return target;
  }

  public void setTarget(Target target) {
    this.target = target;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public void setTargetId(UUID targetId) {
    this.targetId = targetId;
  }

  public String getTargetName() {
    return targetName;
  }

  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Map<String, Object> getChanges() {
    return changes;
  }

  public void setChanges(Map<String, Object> changes) {
    this.changes = changes;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }
}

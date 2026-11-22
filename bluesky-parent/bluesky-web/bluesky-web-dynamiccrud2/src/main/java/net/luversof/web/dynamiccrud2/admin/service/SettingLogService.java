package net.luversof.web.dynamiccrud2.admin.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.MainMenu;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.Project;
import net.luversof.web.dynamiccrud2.domain.SearchSettings;
import net.luversof.web.dynamiccrud2.domain.SettingLog;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Action;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Target;
import net.luversof.web.dynamiccrud2.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud2.repository.MenuRepository;
import net.luversof.web.dynamiccrud2.repository.SettingLogRepository;

/**
 * 설정 변경 이력(SettingLog): 관리 화면의 저장 · 지우기 · 순서 바꾸기마다 "누가 · 언제 · 무엇을 · 이전 → 이후" 를 남기고 조회한다. 사용 화면의 데이터
 * 변경 이력(ChangeLog, {@link ChangeLogService})과는 따로 저장한다.
 *
 * <p>값은 스냅숏(항목 → 문자열)으로 남기며, 바뀐 항목이 없으면 남기지 않는다. 남기기에 실패해도(테이블이 아직 없는 등) 설정 저장은 그대로 둔다.
 */
@Service
public class SettingLogService {

  private static final Logger LOG = LoggerFactory.getLogger(SettingLogService.class);

  public static final int PAGE_SIZE = 20;

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final SettingLogRepository settingLogRepository;
  private final MenuRepository menuRepository;
  private final MainMenuRepository mainMenuRepository;

  public SettingLogService(
      SettingLogRepository settingLogRepository,
      MenuRepository menuRepository,
      MainMenuRepository mainMenuRepository) {
    this.settingLogRepository = settingLogRepository;
    this.menuRepository = menuRepository;
    this.mainMenuRepository = mainMenuRepository;
  }

  // ---- 남기기 ----

  /**
   * 설정 변경 하나를 남긴다.
   *
   * @param before 바꾸기 전 스냅숏(만들기는 null)
   * @param after 바꾼 뒤 스냅숏(지우기는 null)
   * @param projectId 걸러 볼 프로젝트(모르면 null - menuId 가 있으면 메뉴의 프로젝트로 채운다)
   * @param menuId 걸러 볼 메뉴(없으면 null)
   */
  public void record(
      Target target,
      Action action,
      UUID targetId,
      String targetName,
      UUID projectId,
      UUID menuId,
      Map<String, String> before,
      Map<String, String> after) {
    var changes = diff(before, after);
    if (changes.isEmpty() && action == Action.UPDATE) {
      return;
    }
    try {
      var log = new SettingLog();
      log.setTarget(target);
      log.setAction(action);
      log.setTargetId(targetId);
      log.setTargetName(targetName);
      log.setMenuId(menuId);
      log.setProjectId(projectId != null ? projectId : projectOf(menuId));
      log.setUsername(currentUsername());
      log.setChanges(new LinkedHashMap<>(changes));
      settingLogRepository.save(log);
    } catch (RuntimeException e) {
      LOG.error(
          "[SETTING_LOG_FAILED] target={} action={} id={} name={} changes={}",
          target,
          action,
          targetId,
          targetName,
          changes,
          e);
    }
  }

  /** 바뀐 항목만 {항목: {before, after}} (항목 순서는 스냅숏 순서) */
  static Map<String, Map<String, String>> diff(
      Map<String, String> before, Map<String, String> after) {
    var keys = new LinkedHashSet<String>();
    if (before != null) {
      keys.addAll(before.keySet());
    }
    if (after != null) {
      keys.addAll(after.keySet());
    }
    var result = new LinkedHashMap<String, Map<String, String>>();
    for (var key : keys) {
      var old = before == null ? null : before.get(key);
      var now = after == null ? null : after.get(key);
      if (!Objects.equals(old, now)) {
        var change = new LinkedHashMap<String, String>();
        change.put("before", old);
        change.put("after", now);
        result.put(key, change);
      }
    }
    return result;
  }

  private UUID projectOf(UUID menuId) {
    if (menuId == null) {
      return null;
    }
    return menuRepository
        .findById(menuId)
        .flatMap(menu -> mainMenuRepository.findById(menu.getMainMenuId()))
        .map(MainMenu::getProjectId)
        .orElse(null);
  }

  private static String currentUsername() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null ? "unknown" : authentication.getName();
  }

  // ---- 스냅숏(항목 이름은 메시지 키 setting.field.* 의 끝부분) ----

  public static Map<String, String> snapshot(Project project) {
    var map = new LinkedHashMap<String, String>();
    map.put("name", project.getName());
    map.put("code", project.getCode());
    map.put("enableDisplay", String.valueOf(project.isEnableDisplay()));
    return map;
  }

  public static Map<String, String> snapshot(MainMenu mainMenu) {
    var map = new LinkedHashMap<String, String>();
    map.put("name", mainMenu.getName());
    map.put("code", mainMenu.getCode());
    map.put("enableDisplay", String.valueOf(mainMenu.isEnableDisplay()));
    return map;
  }

  public static Map<String, String> snapshot(Menu menu) {
    var map = new LinkedHashMap<String, String>();
    map.put("name", menu.getName());
    map.put("code", menu.getCode());
    map.put("authority", menu.getAuthority());
    map.put("pageSize", String.valueOf(menu.getPageSize()));
    map.put("enableDisplay", String.valueOf(menu.isEnableDisplay()));
    map.put("enableExcel", String.valueOf(menu.isEnableExcel()));
    return map;
  }

  public static Map<String, String> snapshot(MenuQuery query) {
    var map = new LinkedHashMap<String, String>();
    map.put("kind", query.getKind() == null ? null : query.getKind().name());
    map.put("name", query.getName());
    map.put("authority", query.getAuthority());
    map.put("queryText", query.getQueryText());
    map.put(
        "paramSettings",
        query.getParamSettings() == null ? null : canonical(query.getParamSettings()));
    return map;
  }

  /** 맵을 키 순서로 글자로(HashMap 순서 차이로 안 바뀐 설정이 바뀐 것처럼 남지 않게, 안쪽 맵까지) */
  static String canonical(Object value) {
    if (value instanceof Map<?, ?> map) {
      var sorted = new java.util.TreeMap<String, String>();
      map.forEach((k, v) -> sorted.put(String.valueOf(k), canonical(v)));
      return sorted.toString();
    }
    return String.valueOf(value);
  }

  public static Map<String, String> snapshot(SearchSettings settings) {
    var map = new LinkedHashMap<String, String>();
    map.put(
        "searchFields",
        String.join(
            ", ",
            settings.fields().stream()
                .map(f -> f.label() + "(" + f.operator().name() + ")")
                .toList()));
    map.put("keyword", String.join(", ", settings.keyword()));
    return map;
  }

  public static Map<String, String> snapshot(DbField field) {
    var map = new LinkedHashMap<String, String>();
    map.put("label", field.getLabel());
    map.put("inputType", field.getInputType() == null ? null : field.getInputType().name());
    map.put(
        "maxLength", field.getMaxLength() == null ? null : String.valueOf(field.getMaxLength()));
    map.put("sortable", String.valueOf(field.isSortable()));
    map.put("updatable", String.valueOf(field.isUpdatable()));
    map.put("valueOptions", field.getValueOptions());
    map.put("dateFormat", field.getDateFormat());
    return map;
  }

  /** 순서 바꾸기: 바뀐 순서를 이름 목록으로 */
  public static Map<String, String> order(List<String> names) {
    var map = new LinkedHashMap<String, String>();
    map.put("order", String.join(", ", names));
    return map;
  }

  // ---- 조회 ----

  /** 거르기(빈 값 = 안 거름). target = SettingLog.Target 이름. from · to 는 그날 포함 */
  public record Filter(String username, String target, LocalDate from, LocalDate to) {}

  /** 항목 하나의 이전 → 이후(이름은 지금 언어) */
  public record Change(String label, String before, String after) {}

  public record Row(
      String time,
      String username,
      Target target,
      String targetName,
      Action action,
      List<Change> changes) {}

  public record HistoryPage(
      List<Row> rows, long total, int page, int lastPage, List<String> usernames) {}

  /** 메뉴 범위(메뉴 · 쿼리 · 검색 + 그 메뉴 테이블의 필드) 또는 프로젝트 범위(menu 가 null) */
  public HistoryPage page(Menu menu, UUID projectId, Filter filter, int page) {
    var menuId = menu == null ? null : menu.getId();
    var tableId = menu == null ? null : menu.getDbTableId();
    var scopeProject = menu == null ? projectId : null;
    var zone = ZoneId.systemDefault();
    Instant from = filter.from() == null ? null : filter.from().atStartOfDay(zone).toInstant();
    Instant to =
        filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay(zone).toInstant();
    var username = blankToNull(filter.username());
    var target = blankToNull(filter.target());
    var total =
        settingLogRepository.countSearch(menuId, tableId, scopeProject, username, target, from, to);
    var lastPage = total <= 0 ? 0 : (int) ((total - 1) / PAGE_SIZE);
    var current = Math.min(Math.max(page, 0), lastPage);
    var rows = new ArrayList<Row>();
    for (var log :
        settingLogRepository.search(
            menuId,
            tableId,
            scopeProject,
            username,
            target,
            from,
            to,
            PAGE_SIZE,
            (long) current * PAGE_SIZE)) {
      rows.add(
          new Row(
              log.getCreatedDate() == null
                  ? "-"
                  : TIME.format(LocalDateTime.ofInstant(log.getCreatedDate(), zone)),
              log.getUsername(),
              log.getTarget(),
              log.getTargetName(),
              log.getAction(),
              changesOf(log)));
    }
    return new HistoryPage(
        rows,
        total,
        current,
        lastPage,
        settingLogRepository.findUsernames(menuId, tableId, scopeProject));
  }

  @SuppressWarnings("unchecked")
  static List<Change> changesOf(SettingLog log) {
    var result = new ArrayList<Change>();
    if (log.getChanges() == null) {
      return result;
    }
    log.getChanges()
        .forEach((key, value) -> {
          if (value instanceof Map<?, ?> map) {
            var m = (Map<String, Object>) map;
            result.add(new Change(labelOf(key), text(m.get("before")), text(m.get("after"))));
          }
        });
    return result;
  }

  private static String labelOf(String key) {
    try {
      return Msg.t("setting.field." + key);
    } catch (RuntimeException e) {
      return key;
    }
  }

  private static String text(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}

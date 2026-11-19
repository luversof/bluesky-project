package net.luversof.web.dynamiccrud2.admin.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.domain.ChangeLog;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.repository.ChangeLogRepository;

/**
 * 메뉴 관리의 변경 이력 화면: 사용 화면에서 실행한 입력 · 수정 · 삭제(ChangeLog)를 최근 순으로 거르며 본다. 이력에는 넣은 값만 있고 바뀌기 전 값은 없다(전후
 * 비교는 저장 방식을 바꿔야 한다).
 */
@Service
public class ChangeLogService {

  public static final int PAGE_SIZE = 20;

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ChangeLogRepository changeLogRepository;
  private final FieldService fieldService;

  public ChangeLogService(ChangeLogRepository changeLogRepository, FieldService fieldService) {
    this.changeLogRepository = changeLogRepository;
    this.fieldService = fieldService;
  }

  /** 거르기 조건(빈 값 = 안 거름). from · to 는 그날 포함 */
  public record Filter(String username, UUID queryId, LocalDate from, LocalDate to) {}

  /**
   * 이력 한 줄.
   *
   * @param queryName 실행한 쿼리 이름(지운 쿼리면 "(지운 쿼리)")
   * @param kind 쿼리 종류(지운 쿼리면 null)
   * @param rows 실행한 행마다 값 목록(전후 비교)
   */
  public record Row(
      String time,
      String username,
      String queryName,
      QueryKind kind,
      int affectedRows,
      List<List<Value>> rows) {}

  /**
   * 값 하나의 전후. 입력은 after 만, 삭제는 before 만(지운 행 전체), 수정은 둘 다(바뀐 칸은 changed). 전후 비교를 넣기 전(2026-11-19)
   * 이력은 before 가 없다.
   *
   * @param label 필드 라벨(없으면 파라미터 · 목록 제목)
   */
  public record Value(String label, String before, String after, boolean changed) {}

  public record HistoryPage(
      List<Row> rows, long total, int page, int lastPage, List<String> usernames) {}

  public HistoryPage page(Menu menu, Filter filter, int page) {
    var zone = ZoneId.systemDefault();
    Instant from = filter.from() == null ? null : filter.from().atStartOfDay(zone).toInstant();
    Instant to =
        filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay(zone).toInstant();
    var username =
        filter.username() == null || filter.username().isBlank() ? null : filter.username();
    var total = changeLogRepository.countSearch(menu.getId(), username, filter.queryId(), from, to);
    var lastPage = total <= 0 ? 0 : (int) ((total - 1) / PAGE_SIZE);
    var current = Math.min(Math.max(page, 0), lastPage);
    var logs =
        changeLogRepository.search(
            menu.getId(),
            username,
            filter.queryId(),
            from,
            to,
            PAGE_SIZE,
            (long) current * PAGE_SIZE);

    var queries = new HashMap<UUID, MenuQuery>();
    menu.getQueries().forEach(q -> queries.put(q.getId(), q));
    var labels = new HashMap<String, String>();
    fieldService
        .get(menu.getDbTableId())
        .getFields()
        .forEach(f ->
            labels.put(
                f.getColumnName(),
                f.getLabel() == null || f.getLabel().isBlank() ? f.getColumnName() : f.getLabel()));

    var rows = new ArrayList<Row>();
    for (var log : logs) {
      var query = queries.get(log.getMenuQueryId());
      rows.add(
          new Row(
              log.getCreatedDate() == null
                  ? "-"
                  : TIME.format(LocalDateTime.ofInstant(log.getCreatedDate(), zone)),
              log.getUsername(),
              query == null ? Msg.t("admin.history.deletedQuery") : query.getName(),
              query == null ? null : query.getKind(),
              log.getAffectedRows(),
              valuesOf(log, query == null ? null : query.getKind(), labels)));
    }
    return new HistoryPage(
        rows, total, current, lastPage, changeLogRepository.findUsernames(menu.getId()));
  }

  /**
   * bindValues {"rows": [{파라미터: 넣은 값}], "before": [{파라미터: 바뀌기 전 값}], "beforeRows": [{목록 제목: 값}]} →
   * 행마다 값 목록. 삭제는 지운 행 전체(beforeRows), 그 밖은 파라미터마다 넣은 값과 바뀌기 전 값. 이름은 파라미터가 컬럼이면 필드 라벨.
   */
  static List<List<Value>> valuesOf(ChangeLog log, QueryKind kind, Map<String, String> labels) {
    var result = new ArrayList<List<Value>>();
    if (log.getBindValues() == null) {
      return result;
    }
    var rows = maps(log.getBindValues().get("rows"));
    var before = maps(log.getBindValues().get("before"));
    var beforeRows = maps(log.getBindValues().get("beforeRows"));
    if (kind == QueryKind.DELETE && !beforeRows.isEmpty()) {
      for (var row : beforeRows) {
        var values = new ArrayList<Value>();
        row.forEach((label, value) -> values.add(new Value(label, value, null, false)));
        result.add(values);
      }
      return result;
    }
    for (int i = 0; i < rows.size(); i++) {
      var previous = i < before.size() ? before.get(i) : Map.<String, String>of();
      var values = new ArrayList<Value>();
      rows.get(i)
          .forEach((name, after) -> {
            var old = previous.get(name);
            values.add(
                new Value(
                    labels.getOrDefault(name, name),
                    old,
                    after,
                    previous.containsKey(name) && !java.util.Objects.equals(old, after)));
          });
      result.add(values);
    }
    return result;
  }

  /** JSON 배열 → [이름: 문자열 값] 목록(모양이 다르면 빈 목록) */
  private static List<Map<String, String>> maps(Object value) {
    var result = new ArrayList<Map<String, String>>();
    if (!(value instanceof List<?> list)) {
      return result;
    }
    for (var item : list) {
      if (item instanceof Map<?, ?> map) {
        var row = new LinkedHashMap<String, String>();
        map.forEach((k, v) -> row.put(String.valueOf(k), v == null ? "" : String.valueOf(v)));
        result.add(row);
      }
    }
    return result;
  }
}

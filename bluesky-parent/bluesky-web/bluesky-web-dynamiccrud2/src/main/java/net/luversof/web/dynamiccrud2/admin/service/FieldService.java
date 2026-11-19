package net.luversof.web.dynamiccrud2.admin.service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud2.adapter.ColumnMeta;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.DbTable;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Action;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Target;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;

/**
 * 테이블 단위 필드(DbTable · DbField) 등록 · 동기화. DB 에서 온 값(타입 · 주석 · 자동 증가)은 다시 읽을 때 갱신하고, 사용자가 고치는 값(라벨 ·
 * 입력 형식 · 필수 · 최대 길이)은 유지한다. 정렬 허용은 사용자가 바꾼 적 없을 때만 인덱스로 다시 정한다.
 */
@Service
public class FieldService {

  private final DbTableRepository dbTableRepository;
  private final DatabaseAdapterResolver resolver;
  private final SettingLogService settingLog;

  public FieldService(
      DbTableRepository dbTableRepository,
      DatabaseAdapterResolver resolver,
      SettingLogService settingLog) {
    this.dbTableRepository = dbTableRepository;
    this.resolver = resolver;
    this.settingLog = settingLog;
  }

  public DbTable get(UUID dbTableId) {
    return dbTableRepository.findById(dbTableId).orElseThrow();
  }

  /**
   * 테이블을 처음 쓰면 컬럼 전체를 등록하고, 이미 있으면(메뉴끼리 공유) 방금 읽은 컬럼으로 다시 맞춘다. 등록 뒤 DB 에 늘어난 컬럼이 필드에 없으면 자동 생성 쿼리에는
   * 들어가는데 타입 변환 없이 문자열로 바인딩돼 실패하기 때문(예: boolean 컬럼). 사용자가 고친 값은 유지된다.
   */
  public DbTable registerOrGet(String connectionName, TableMeta meta) {
    var existing = find(connectionName, meta.ref());
    if (existing != null) {
      sync(existing, meta, null);
      return dbTableRepository.save(existing);
    }
    var dbTable = newTable(connectionName, meta.ref());
    sync(dbTable, meta, null);
    return dbTableRepository.save(dbTable);
  }

  /** "컬럼 다시 읽기" */
  public DbTable resync(UUID dbTableId) {
    var dbTable = get(dbTableId);
    sync(dbTable, readMeta(dbTable), null);
    return dbTableRepository.save(dbTable);
  }

  /** 지금 DB 의 컬럼 · 인덱스(필드 관리 화면의 인덱스 표 · 정렬 안내) */
  public TableMeta readMeta(DbTable dbTable) {
    return resolver
        .resolve(dbTable.getConnectionName())
        .readTable(
            new TableRef(
                dbTable.getCatalogName(), dbTable.getSchemaName(), dbTable.getTableName()));
  }

  /** 조인한 다른 테이블의 실제 컬럼: 그 테이블을 쓰인 컬럼만으로 등록(또는 빠진 컬럼만 추가)해 그 테이블 메뉴들과 라벨을 공유한다. */
  public DbTable ensureColumns(
      String connectionName, TableRef ref, Collection<String> columnNames) {
    var dbTable = find(connectionName, ref);
    if (dbTable != null
        && dbTable.getFields().stream()
            .map(DbField::getColumnName)
            .toList()
            .containsAll(columnNames)) {
      return dbTable;
    }
    var meta = resolver.resolve(connectionName).readTable(ref);
    if (dbTable == null) {
      dbTable = newTable(connectionName, meta.ref());
    }
    sync(dbTable, meta, columnNames);
    return dbTableRepository.save(dbTable);
  }

  /** 계산 · 함수 결과: 메뉴 테이블의 가상 필드(조회 전용)로 등록 */
  public DbTable ensureVirtual(UUID dbTableId, Collection<String> names) {
    var dbTable = get(dbTableId);
    var existing = new HashMap<String, DbField>();
    dbTable.getFields().forEach(f -> existing.put(f.getColumnName(), f));
    var order = dbTable.getFields().size();
    var changed = false;
    for (var name : names) {
      if (existing.containsKey(name)) {
        continue;
      }
      var field = new DbField();
      field.setColumnName(name);
      field.setVirtual(true);
      field.setInputType(InputType.TEXT);
      field.setDisplayOrder(order++);
      dbTable.getFields().add(field);
      changed = true;
    }
    return changed ? dbTableRepository.save(dbTable) : dbTable;
  }

  /**
   * 필드 화면에서 사용자가 고친 값. "필수" 는 받지 않는다 - 입력 필수는 작업마다 다를 수 있어(입력은 필수, 수정은 선택) 쿼리 화면 파라미터 설정 한 곳에서
   * 정한다(2026-11-19 사용자 결정, 필드의 required 는 DB NOT NULL 기록으로만 남음)
   */
  public DbTable updateField(
      UUID dbTableId,
      String columnName,
      String label,
      InputType inputType,
      Integer maxLength,
      boolean sortable,
      boolean updatable) {
    var dbTable = get(dbTableId);
    var field =
        dbTable.getFields().stream()
            .filter(f -> f.getColumnName().equals(columnName))
            .findFirst()
            .orElseThrow();
    var before = SettingLogService.snapshot(field);
    field.setLabel(label == null || label.isBlank() ? null : label.strip());
    field.setInputType(inputType);
    field.setMaxLength(maxLength);
    if (field.isSortable() != sortable) {
      field.setSortable(sortable && !field.isVirtual());
      field.setSortableUserSet(true);
    }
    if (field.isUpdatable() != updatable) {
      field.setUpdatable(updatable);
      field.setUpdatableUserSet(true);
    }
    var saved = dbTableRepository.save(dbTable);
    // 필드는 테이블 단위(여러 메뉴 공유): 대상 id = 테이블, 메뉴 이력에서는 그 메뉴 테이블의 필드 변경으로 보인다
    settingLog.record(
        Target.FIELD,
        Action.UPDATE,
        dbTableId,
        dbTable.getTableName() + "." + columnName,
        null,
        null,
        before,
        SettingLogService.snapshot(field));
    return saved;
  }

  private DbTable find(String connectionName, TableRef ref) {
    return dbTableRepository
        .findIdByLocation(connectionName, ref.catalog(), ref.schema(), ref.name())
        .flatMap(dbTableRepository::findById)
        .orElse(null);
  }

  private static DbTable newTable(String connectionName, TableRef ref) {
    var dbTable = new DbTable();
    dbTable.setConnectionName(connectionName);
    dbTable.setCatalogName(ref.catalog());
    dbTable.setSchemaName(ref.schema());
    dbTable.setTableName(ref.name());
    return dbTable;
  }

  /**
   * DB 정보를 필드에 반영한다.
   *
   * @param onlyColumns null 이면 컬럼 전체, 아니면 이 컬럼만 새로 추가(기존 필드 갱신은 항상)
   */
  private static void sync(DbTable dbTable, TableMeta meta, Collection<String> onlyColumns) {
    var existing = new HashMap<String, DbField>();
    dbTable.getFields().forEach(f -> existing.put(f.getColumnName(), f));
    var seen = new java.util.HashSet<String>();
    var order = 0;
    for (ColumnMeta column : meta.columns()) {
      seen.add(column.name());
      var field = existing.get(column.name());
      var sortable = meta.indexLeadingColumns().contains(column.name());
      // 행을 가리키는 값(PK · unique)과 DB 가 만드는 값(자동 증가)은 수정 작업에서 바꾸지 않는다
      var updatable = !meta.isKeyColumn(column.name()) && !column.autoIncrement();
      if (field == null) {
        if (onlyColumns != null && !onlyColumns.contains(column.name())) {
          order++;
          continue;
        }
        field = new DbField();
        field.setColumnName(column.name());
        field.setInputType(column.inputType());
        field.setRequired(column.required());
        field.setMaxLength(column.maxLength());
        field.setSortable(sortable);
        dbTable.getFields().add(field);
      } else if (!field.isSortableUserSet()) {
        field.setSortable(sortable);
      }
      if (!field.isUpdatableUserSet()) {
        field.setUpdatable(updatable);
      }
      field.setDataType(column.typeName());
      field.setDbComment(column.comment());
      field.setAutoIncrement(column.autoIncrement());
      field.setMissingInDb(false);
      field.setDisplayOrder(order++);
    }
    for (var field : dbTable.getFields()) {
      if (!field.isVirtual() && !seen.contains(field.getColumnName())) {
        field.setMissingInDb(true);
      }
    }
    dbTable.setColumnsReadDate(Instant.now());
  }

  /**
   * 화면 제목: 필드 라벨 → 쿼리 별칭 → DB 주석 → 컬럼 이름(fallback). 제목은 테이블 필드 관리 한 곳에서 정하고, 별칭은 라벨이 없을 때만 제목이
   * 된다(2026-11-19 사용자 결정 - 예전엔 별칭이 라벨을 말없이 덮었다).
   */
  public static String headerOf(DbField field, String alias, String fallback) {
    if (field != null && field.getLabel() != null && !field.getLabel().isBlank()) {
      return field.getLabel();
    }
    if (alias != null) {
      return alias;
    }
    if (field == null) {
      return fallback;
    }
    if (field.getDbComment() != null && !field.getDbComment().isBlank()) {
      return field.getDbComment();
    }
    return field.getColumnName();
  }

  /** 조회 결과 컬럼의 필드를 찾는다. 한 화면을 그리는 동안 쓰고 버린다(조인한 다른 테이블은 한 번만 읽는다) */
  public Lookup lookup(DbTable base) {
    return new Lookup(base);
  }

  public final class Lookup {

    private final DbTable base;
    private final java.util.Map<String, java.util.Optional<DbTable>> others = new HashMap<>();

    private Lookup(DbTable base) {
      this.base = base;
    }

    /** 메뉴 테이블이면 그 필드, 조인한 다른 테이블이면 등록된 그 테이블 필드, 계산 결과면 메뉴 테이블의 가상 필드 */
    public DbField fieldOf(ResultColumn column) {
      var name =
          column.kind() == ResultColumn.Kind.EXPRESSION ? column.label() : column.columnName();
      return tableOf(column)
          .flatMap(
              t -> t.getFields().stream().filter(f -> f.getColumnName().equals(name)).findFirst())
          .orElse(null);
    }

    /** 필드가 있는 테이블(필드 관리 화면 링크용). 등록되지 않은 다른 테이블이면 빈 값 */
    public java.util.Optional<DbTable> tableOf(ResultColumn column) {
      if (column.kind() == ResultColumn.Kind.EXPRESSION
          || column.tableName() == null
          || column.tableName().equalsIgnoreCase(base.getTableName())) {
        return java.util.Optional.of(base);
      }
      return others.computeIfAbsent(
          column.tableName().toLowerCase(),
          key ->
              dbTableRepository
                  .findIdByLocation(
                      base.getConnectionName(),
                      base.getCatalogName(),
                      base.schemaOf(column.tableSchema()),
                      column.tableName())
                  .flatMap(dbTableRepository::findById));
    }
  }
}

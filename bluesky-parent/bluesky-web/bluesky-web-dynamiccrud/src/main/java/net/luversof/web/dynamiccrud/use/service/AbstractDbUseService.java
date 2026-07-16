package net.luversof.web.dynamiccrud.use.service;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldSearchType;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.util.DynamicTableUtil;
import net.luversof.web.dynamiccrud.setting.util.JSqlParserUtil;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.support.DynamicCrudSettingTransactionHandler;
import net.luversof.web.dynamiccrud.use.domain.BulkQueryResult;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

/** 현재 MySql과 MsSql의 경우 기능이 거의 동일하여 상위 service를 구성함 */
public abstract class AbstractDbUseService implements UseService {

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @Autowired private DynamicCrudSettingTransactionHandler dynamicCrudSettingTransactionHandler;

  private static final RowMapper<Map<String, Object>> ROW_MAPPER = new ColumnMapRowMapper();

  protected abstract void addPagingCondition(PlainSelect plainSelect, int limit, long offset);

  @Override
  public Page<Map<String, Object>> find(
      SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap) {
    var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.SELECT);
    var dbFieldList = SettingUtil.getDbFieldList(settingParameter);

    RoutingDataSourceContextHolder.setContext(() -> dbQuery.getDataSourceName());

    // 동적 테이블명(TABLE_NAME DbField)이 설정된 경우 파싱 전에 :columnId placeholder를 검증된 테이블명으로 치환.
    // 테이블이 선택되지 않았으면 DYNAMIC_TABLE_NAME_REQUIRED를 던져 "테이블을 선택해 주세요." 안내를 표시한다.
    var queryString =
        DynamicTableUtil.resolveQueryString(dbQuery.getQueryString(), dbFieldList, dataMap);

    // count, paging query를 만들기 위해 생성
    PlainSelect selectQuery;
    PlainSelect countQuery;
    try {
      selectQuery = (PlainSelect) CCJSqlParserUtil.parse(queryString);
      countQuery = (PlainSelect) CCJSqlParserUtil.parse(queryString);
    } catch (JSQLParserException e) {
      throw new RuntimeException(e);
    }

    // 동적 테이블명 지정용 필드는 검색 조건(WHERE)이 아니므로 검색 처리 대상에서 제외
    var searchFieldList =
        dbFieldList.stream()
            .filter(x -> !DbFieldColumnType.TABLE_NAME.equals(x.getColumnType()))
            .toList();

    // 조건에 따라 처리를 하기 위해 3개 조건을 모두 가져와야 함.
    var dbQueryWhereClauseColumnNameList =
        JSqlParserUtil.findWhereClauseColumnNameList(selectQuery);
    var dbQueryWhereClauseNamedParameterNameList =
        JSqlParserUtil.findWhereClauseNamedParameterNameList(selectQuery);
    var dbFieldSearchRequiredList =
        searchFieldList.stream()
            .filter(x -> DbFieldEnable.REQUIRED.equals(x.getEnableSearch()))
            .toList();
    var dbFieldSearchEnabledList =
        searchFieldList.stream()
            .filter(x -> DbFieldEnable.ENABLED.equals(x.getEnableSearch()))
            .toList();
    var dbFieldSearchDisabledList =
        searchFieldList.stream()
            .filter(x -> DbFieldEnable.DISABLED.equals(x.getEnableSearch()))
            .toList();

    // dbField의 Required의 경우
    if (!dbFieldSearchRequiredList.isEmpty()) {
      for (var dbField : dbFieldSearchRequiredList) {
        // 전달받은 parameter가 없어도 dbQuery에 등록된 where 절에 columnName이 있고 namedParameter가 없으면
        // 고정값으로 간주하고 허용
        if (dbQueryWhereClauseColumnNameList.contains(dbField.getColumnId())
            && !dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())) {
          continue;
        }

        // parameter에 해당 값이 없으면 빈 값 반환
        if (!dataMap.containsKey(dbField.getColumnId())
            || !StringUtils.hasText(dataMap.get(dbField.getColumnId()))) {
          return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // dbQuery에 해당하는 dbQuery에 등록된 where 절이 없다면 추가
        if (!dbQueryWhereClauseColumnNameList.contains(dbField.getColumnId())) {
          var whereClauseAppendExpression =
              JSqlParserUtil.createWhereClauseAppendExpression(dbField);
          JSqlParserUtil.appendWhereCondition(selectQuery, whereClauseAppendExpression);
          JSqlParserUtil.appendWhereCondition(countQuery, whereClauseAppendExpression);
        }
      }
    }

    // dbField Enabled의 경우
    if (!dbFieldSearchEnabledList.isEmpty()) {
      for (var dbField : dbFieldSearchEnabledList) {
        // parameter가 없는데 dbQuery에 해당 namedParameter가 있으면 관련 조건 삭제
        var hasParameter =
            dataMap.containsKey(dbField.getColumnId())
                && StringUtils.hasText(dataMap.get(dbField.getColumnId()));
        if (!hasParameter
            && dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())) {
          JSqlParserUtil.removeWhereClauseByNamedParameterName(selectQuery, dbField.getColumnId());
          JSqlParserUtil.removeWhereClauseByNamedParameterName(countQuery, dbField.getColumnId());
        }

        // parameter가 있는데 dbQuery에 해당 namedParameter가 없으면 관련 조건 추가
        if (hasParameter
            && !dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())) {
          var whereClauseAppendExpression =
              JSqlParserUtil.createWhereClauseAppendExpression(dbField);
          JSqlParserUtil.appendWhereCondition(selectQuery, whereClauseAppendExpression);
          JSqlParserUtil.appendWhereCondition(countQuery, whereClauseAppendExpression);
        }
      }
    }

    // dbField가 Disabled인데 dbQuery에 해당 namedParameter가 있으면 관련 조건 삭제
    if (!dbFieldSearchDisabledList.isEmpty()) {
      for (var dbField : dbFieldSearchDisabledList) {
        if ((!dataMap.containsKey(dbField.getColumnId())
                || !StringUtils.hasText(dataMap.get(dbField.getColumnId())))
            && dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())) {
          JSqlParserUtil.removeWhereClauseByNamedParameterName(selectQuery, dbField.getColumnId());
          JSqlParserUtil.removeWhereClauseByNamedParameterName(countQuery, dbField.getColumnId());
        }
      }
    }

    // selectQuery는 페이징을 위해 limit offset 설정을 추가한다.
    // 만약 limit offset이 쿼리에 등록되어 있어도 해당 설정을 지우고 추가함
    addPagingCondition(selectQuery, pageable.getPageSize(), pageable.getOffset());

    var paramSource = new MapSqlParameterSource();
    paramSource.addValue("limit", pageable.getPageSize());
    paramSource.addValue("offset", pageable.getOffset());

    // SPEL_FOR_EDIT 같이 추가 처리된 값을 설정하기 위해 일괄 처리
    // 검색 조건이 like인 경우 문자열 처리 추가
    dataMap.forEach(
        (key, value) ->
            paramSource.addValue(key, getMapSqlParameterSourceValue(key, value, dbFieldList)));

    // customQuery 조건에 대해 검토 필요
    // 이 부분은 일단 주석 처리함
    // if (SettingStringUtil.isCustomQuery(dbQuery.getQueryString())) {
    // List<Map<String, Object>> contentList =
    // dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() ->
    // namedParameterJdbcTemplate.query(dbQuery.getQueryString(), paramSource,
    // ROW_MAPPER));
    // var customPageable = PageRequest.of(0, contentList.size() <= 0 ? 1 :
    // contentList.size(), pageable.getSort());
    // return new PageImpl<>(contentList, customPageable, contentList.size());
    // }

    List<Map<String, Object>> contentList =
        dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(
            () ->
                namedParameterJdbcTemplate.query(selectQuery.toString(), paramSource, ROW_MAPPER));

    // 첫페이지 호출에 pageSize보다 결과 값이 적은 경우 count 호출이 불필요함
    if (pageable.getOffset() == 0 && contentList.size() < pageable.getPageSize()) {
      return new PageImpl<>(contentList, pageable, contentList.size());
    }

    // count query 조회
    // countQuery column 부분 변경
    {
      countQuery.getSelectItems().clear();
      var function = new Function();
      function.setName("count");
      function.setParameters(new AllColumns());
      countQuery.getSelectItems().add(new SelectItem<>(function));
    }

    // countQuery의 경우 order by 절 제거
    countQuery.setOrderByElements(null);

    int totalCount =
        dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(
            () ->
                namedParameterJdbcTemplate.queryForObject(
                    countQuery.toString(), paramSource, Integer.class));
    return new PageImpl<>(contentList, pageable, totalCount);
  }

  @Override
  public Object create(SettingParameter settingParameter, Map<String, String> dataMap) {
    var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.INSERT);
    var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
    var queryString =
        DynamicTableUtil.resolveQueryString(dbQuery.getQueryString(), dbFieldList, dataMap);
    return jdbcTemplateUpdate(dbQuery, queryString, dbFieldList, dataMap);
  }

  /** insert/update query는 등록된 쿼리를 그대로 실행하고 넘겨받은 postData만 설정함 */
  @Override
  public Object update(SettingParameter settingParameter, Map<String, String> dataMap) {
    var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.UPDATE);
    var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
    var queryString =
        DynamicTableUtil.resolveQueryString(dbQuery.getQueryString(), dbFieldList, dataMap);
    return jdbcTemplateUpdate(dbQuery, queryString, dbFieldList, dataMap);
  }

  /** Delete의 경우 여러 건을 동시에 삭제할 수 있음. 삭제도 update 쿼리를 통해 수행함 */
  @Override
  public Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap) {
    var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.DELETE);
    var dbFieldList = SettingUtil.getDbFieldList(settingParameter);

    // 테이블명은 삭제 대상 여러 건에 공통이므로 건별 반복 이전에 한 번만 치환
    var queryString =
        DynamicTableUtil.resolveQueryString(
            dbQuery.getQueryString(), dbFieldList, dataMap.toSingleValueMap());

    List<Map<String, String>> dataMapList = new ArrayList<>();

    dataMap.forEach(
        (key, value) -> {
          // 갯수 만큼 맵을 추가한다.
          if (dataMapList.isEmpty()) {
            for (int i = 0; i < value.size(); i++) {
              dataMapList.add(new HashMap<String, String>());
            }
          }

          for (int i = 0; i < value.size(); i++) {
            dataMapList.get(i).put(key, value.get(i));
          }
        });

    List<Object> resultList = new ArrayList<Object>();
    dataMapList.forEach(
        map -> {
          Object result = jdbcTemplateUpdate(dbQuery, queryString, dbFieldList, map);
          resultList.add(result);
        });
    return resultList;
  }
  
	/**
	 * 사용자가 입력한 임의 SQL을 실행한다(벌크 쿼리 실행 기능).
	 * DataSource는 해당 subMenu에 등록된 SELECT 쿼리의 연결명을 그대로 사용한다.
	 * JSqlParser로 파싱해 SELECT면 결과 리스트, 그 외(DML)면 영향 행수를 반환한다.
	 * 파싱 실패(잘못된 SQL)는 예외로 던져 호출부에서 사용자에게 표시한다.
	 * ⚠️ 임의 SQL 실행이므로 호출부(컨트롤러)에서 ROLE_MASTER/ROLE_ADMIN 권한을 반드시 검증할 것.
	 * @throws JSQLParserException 
	 */
	@Override
	public Object executeRawQuery(SettingParameter settingParameter, String sql) throws JSQLParserException {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.SELECT);
		RoutingDataSourceContextHolder.setContext(dbQuery::getDataSourceName);

		// JDBC는 한 번에 여러 문장(';'로 구분)을 실행하지 못하므로, 입력을 문장 단위로 파싱해 순차 실행한다.
		// (파싱 실패는 예외로 던져 호출부에서 전체 오류로 표시)
		var statements = CCJSqlParserUtil.parseStatements(sql);
		var results = new ArrayList<BulkQueryResult>();

		for (var statement : statements.getStatements()) {
			var single = statement.toString();
			try {
				if (statement instanceof net.sf.jsqlparser.statement.select.Select) {
					List<Map<String, Object>> rows = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(
							() -> namedParameterJdbcTemplate.query(single, new MapSqlParameterSource(), ROW_MAPPER));
					results.add(new BulkQueryResult(single, "select", rows, null, null));
				} else {
					Integer affected = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(
							() -> namedParameterJdbcTemplate.update(single, new MapSqlParameterSource()));
					results.add(new BulkQueryResult(single, "update", null, affected, null));
				}
			} catch (Exception e) {
				// 문장별 독립 실행이므로 오류 발생 시 이후 문장은 실행하지 않고 중단한다(부분 실행 확대 방지).
				results.add(new BulkQueryResult(single, "error", null, null, e.getMessage()));
				break;
			}
		}
		return results;
	}

  /**
   * jdbcTemplate은 insert, update, delete를 update method로 동일하게 수행 전달받은 dataMap을 기준으로 paramSource를 구성
   */
  private Object jdbcTemplateUpdate(
      DbQuery dbQuery, String queryString, List<DbField> dbFieldList, Map<String, String> dataMap) {
    RoutingDataSourceContextHolder.setContext(() -> dbQuery.getDataSourceName());
    var insertQueryBuilder = new StringBuilder(queryString + " ");
    var paramSource = new MapSqlParameterSource();
    setSqlParameterSourceRegisterSqlType(paramSource, dbFieldList);

    dataMap.forEach(
        (key, value) -> {
          if (StringUtils.hasText(value)) {
            paramSource.addValue(key, value);
          } else {
            DbField filterDbField =
                dbFieldList.stream()
                    .filter(dbField -> dbField.getColumnId().equals(key))
                    .findAny()
                    .orElse(null);
            if (filterDbField != null) {
              if (StringUtils.hasText(filterDbField.getColumnDefaultValue())) {
                paramSource.addValue(key, filterDbField.getColumnDefaultValue());
              } else if (filterDbField.getColumnType() == DbFieldColumnType.INT
                  || filterDbField.getColumnType() == DbFieldColumnType.LONG) {
                paramSource.addValue(key, 0); // 숫자형인데 값이 없으면 0으로 기본값 할당
              } else {
                paramSource.addValue(key, null);
              }
            } else {
              paramSource.addValue(key, null);
            }
          }
        });

    return dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(
        () -> namedParameterJdbcTemplate.update(insertQueryBuilder.toString(), paramSource));
  }

  private void setSqlParameterSourceRegisterSqlType(
      MapSqlParameterSource paramSource, List<DbField> dbFieldList) {
    dbFieldList.forEach(
        dbField -> {
          if (dbField.getColumnType().equals(DbFieldColumnType.BOOLEAN)) {
            paramSource.registerSqlType(dbField.getColumnId(), JDBCType.BIT.getVendorTypeNumber());
          }
        });
  }

  /**
   * dbFieldList ColumnSearchType이 like Condition인 경우 value의 앞 또는 앞,뒤에 '%'를 붙이는 처리를 추가
   *
   * @param key
   * @param value
   * @param dbFieldList
   * @return
   */
  private String getMapSqlParameterSourceValue(
      String key, String value, List<DbField> dbFieldList) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    if (dbFieldList == null) {
      return value;
    }
    for (var dbField : dbFieldList) {
      if (dbField.getColumnId().equals(key)) {
        if (DbFieldSearchType.LIKE_RIGHT.equals(dbField.getColumnSearchType())) {
          return value + "%";
        } else if (DbFieldSearchType.LIKE_CONTAINS.equals(dbField.getColumnSearchType())) {
          return '%' + value + "%";
        }
      }
    }

    return value;
  }
}

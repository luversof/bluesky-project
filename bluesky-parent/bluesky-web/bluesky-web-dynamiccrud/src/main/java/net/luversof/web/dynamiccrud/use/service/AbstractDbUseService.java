package net.luversof.web.dynamiccrud.use.service;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import lombok.SneakyThrows;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.util.JSqlParserUtil;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.support.DynamicCrudSettingTransactionHandler;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

/**
 * 현재 MySql과 MsSql의 경우 기능이 거의 동일하여 상위 service를 구성함 
 */
public abstract class AbstractDbUseService implements UseService {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private DynamicCrudSettingTransactionHandler dynamicCrudSettingTransactionHandler;
	
	private static final RowMapper<Map<String, Object>> ROW_MAPPER = new ColumnMapRowMapper();
	
	protected abstract void addPagingCondition(PlainSelect plainSelect, int limit, long offset);
	
	@SneakyThrows
	@Override
	public Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.SELECT);
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		
		RoutingDataSourceContextHolder.setContext(() -> dbQuery.getDataSourceName());
		
		// count, paging query를 만들기 위해 생성
		var selectQuery = (PlainSelect) CCJSqlParserUtil.parse(dbQuery.getQueryString());
		var countQuery = (PlainSelect) CCJSqlParserUtil.parse(dbQuery.getQueryString());
		
		// 조건에 따라 처리를 하기 위해 3개 조건을 모두 가져와야 함.
		var dbQueryWhereClauseColumnNameList = JSqlParserUtil.findWhereClauseColumnNameList(selectQuery);
		var dbQueryWhereClauseNamedParameterNameList = JSqlParserUtil.findWhereClauseNamedParameterNameList(selectQuery);
		var dbFieldSearchRequiredList = dbFieldList.stream().filter(x -> DbFieldEnable.REQUIRED.equals(x.getEnableSearch())).collect(Collectors.toList());
		var dbFieldSearchEnabledList = dbFieldList.stream().filter(x -> DbFieldEnable.ENABLED.equals(x.getEnableSearch())).collect(Collectors.toList());
		var dbFieldSearchDisabledList = dbFieldList.stream().filter(x -> DbFieldEnable.DISABLED.equals(x.getEnableSearch())).collect(Collectors.toList());
		
//		// dbQuery의 namedParameter에 대해 확인
//		if (!dbQueryWhereClauseNamedParameterNameList.isEmpty()) {
//			for (String namedParameterName : dbQueryWhereClauseNamedParameterNameList) {
//				// parameter에 해당 값이 없으면 빈 값 반환
//				if (!dataMap.containsKey(namedParameterName) || !StringUtils.hasText(dataMap.get(namedParameterName))) {
//					return new PageImpl<>(Collections.emptyList(), pageable, 0);
//				}
//			}
//		}
		
		// dbField의 Required의 경우
		if (!dbFieldSearchRequiredList.isEmpty()) {
			for (var dbField : dbFieldSearchRequiredList) {
				// 전달받은 parameter가 없어도 dbQuery에 등록된 where 절에 columnName이 있고 namedParameter가 없으면 고정값으로 간주하고 허용
				if (dbQueryWhereClauseColumnNameList.contains(dbField.getColumnId()) && !dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())) {
					continue;
				}
				
				// parameter에 해당 값이 없으면 빈 값 반환
				if (!dataMap.containsKey(dbField.getColumnId()) || !StringUtils.hasText(dataMap.get(dbField.getColumnId()))) {
					return new PageImpl<>(Collections.emptyList(), pageable, 0);
				}
				
				// dbQuery에 해당하는 dbQuery에 등록된 where 절이 없다면 추가
				if (!dbQueryWhereClauseColumnNameList.contains(dbField.getColumnId())) {
					var whereClauseAppendExpression = JSqlParserUtil.createWhereClauseAppendExpression(dbField);
					JSqlParserUtil.appendWhereCondition(selectQuery, whereClauseAppendExpression);
					JSqlParserUtil.appendWhereCondition(countQuery, whereClauseAppendExpression);
				}
			}
		}
		
		// dbField Enabled의 경우
		if (!dbFieldSearchEnabledList.isEmpty()) {
			for (var dbField : dbFieldSearchEnabledList) {
				// parameter가 없는데 dbQuery에 해당 namedParameter가 있으면 관련 조건 삭제
				var hasParameter = dataMap.containsKey(dbField.getColumnId()) && StringUtils.hasText(dataMap.get(dbField.getColumnId())); 
				if (!hasParameter && dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())) {
					JSqlParserUtil.removeWhereClauseByNamedParameterName(selectQuery, dbField.getColumnId());
					JSqlParserUtil.removeWhereClauseByNamedParameterName(countQuery, dbField.getColumnId());
				}
				
				// parameter가 있는데 dbQuery에 해당 namedParameter가 없으면 관련 조건 추가
				if (hasParameter && !dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())) {
					var whereClauseAppendExpression = JSqlParserUtil.createWhereClauseAppendExpression(dbField);
					JSqlParserUtil.appendWhereCondition(selectQuery, whereClauseAppendExpression);
					JSqlParserUtil.appendWhereCondition(countQuery, whereClauseAppendExpression);
				}
			}
		}
		
		// dbField가 Disabled인데 dbQuery에 해당 namedParameter가 있으면 관련 조건 삭제
		if (!dbFieldSearchDisabledList.isEmpty()) {
			for (var dbField : dbFieldSearchDisabledList) {
				if (
					(!dataMap.containsKey(dbField.getColumnId()) || !StringUtils.hasText(dataMap.get(dbField.getColumnId())))
					&& dbQueryWhereClauseNamedParameterNameList.contains(dbField.getColumnId())
				) {
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
		dataMap.forEach((key, value) -> paramSource.addValue(key, StringUtils.hasText(value) ? value : null));
		
		// customQuery 조건에 대해 검토 필요
		// 이 부분은 일단 주석 처리함
//		if (SettingStringUtil.isCustomQuery(dbQuery.getQueryString())) {
//			List<Map<String, Object>> contentList = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.query(dbQuery.getQueryString(), paramSource, ROW_MAPPER));
//			var customPageable = PageRequest.of(0, contentList.size() <= 0 ? 1 : contentList.size(), pageable.getSort());
//			return new PageImpl<>(contentList, customPageable, contentList.size());
//		}
		
		
		List<Map<String, Object>> contentList = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.query(selectQuery.toString(), paramSource, ROW_MAPPER));
		
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
			countQuery.getSelectItems().add(new SelectItem<Function>(function));
		}
		
		// countQuery의 경우 order by 절 제거
		countQuery.setOrderByElements(null);
		
		int totalCount = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.queryForObject(countQuery.toString(), paramSource, Integer.class));
		return new PageImpl<>(contentList, pageable, totalCount);
	}
	
	
	@Override
	public Object create(SettingParameter settingParameter, Map<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.INSERT);
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		return jdbcTemplateUpdate(dbQuery, dbFieldList, dataMap);
	}

	/**
	 * insert/update query는 등록된 쿼리를 그대로 실행하고 넘겨받은 postData만 설정함
	 */
	@Override
	public Object update(SettingParameter settingParameter, Map<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.UPDATE);
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		return jdbcTemplateUpdate(dbQuery, dbFieldList, dataMap);
	}

	
	/**
	 * Delete의 경우 여러 건을 동시에 삭제할 수 있음.
	 * 삭제도 update 쿼리를 통해 수행함
	 */
	@Override
	public Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.DELETE);
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		
		List<Map<String, String>> dataMapList = new ArrayList<>();
		
		dataMap.forEach((key, value) -> {
			// 갯수 만큼 맵을 추가한다.
			if (dataMapList.isEmpty()) {
				for (int i = 0; i < value.size() ; i++) {
					dataMapList.add(new HashMap<String, String>());
				}
			}
			
			for (int i = 0 ; i < value.size() ; i++) {
				dataMapList.get(i).put(key, value.get(i));
			}
		});

		List<Object> resultList = new ArrayList<Object>();
		dataMapList.forEach(map -> {
			Object result = jdbcTemplateUpdate(dbQuery, dbFieldList, map);
			resultList.add(result);
		});
		return resultList;	
	}
	
	
	/**
	 * jdbcTemplate은 insert, update, delete를  update method로 동일하게 수행
	 * 전달받은 dataMap을 기준으로 paramSource를 구성
	 */
	private Object jdbcTemplateUpdate(DbQuery dbQuery, List<DbField> dbFieldList, Map<String, String> dataMap) {
		RoutingDataSourceContextHolder.setContext(() -> dbQuery.getDataSourceName());
		var insertQueryBuilder = new StringBuilder(dbQuery.getQueryString() + " ");
		var paramSource = new MapSqlParameterSource();
		setSqlParameterSourceRegisterSqlType(paramSource, dbFieldList);
		dataMap.forEach((key, value) -> paramSource.addValue(key, StringUtils.hasText(value) ? value : null));
		return dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.update(insertQueryBuilder.toString(), paramSource));
	}
	
	private void setSqlParameterSourceRegisterSqlType(MapSqlParameterSource paramSource, List<DbField> dbFieldList) {
		dbFieldList.forEach(dbField -> {
			if (dbField.getColumnType().equals(DbFieldColumnType.BOOLEAN)) {
				paramSource.registerSqlType(dbField.getColumnId(), JDBCType.BIT.getVendorTypeNumber());
			}
		});
	}
	
}

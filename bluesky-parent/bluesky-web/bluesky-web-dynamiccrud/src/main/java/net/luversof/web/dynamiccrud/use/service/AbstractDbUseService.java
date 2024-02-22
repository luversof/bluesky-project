package net.luversof.web.dynamiccrud.use.service;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.support.DynamicCrudSettingTransactionHandler;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

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
		
		
		// 필수 검색 조건이 있는 경우 확인
		if (dbFieldList
				.stream()
				.filter(x -> DbFieldEnable.REQUIRED.equals(x.getEnableSearch()))
				.anyMatch(x -> !dataMap.containsKey(x.getColumnId()) || !StringUtils.hasText(dataMap.get(x.getColumnId())))) {
			return new PageImpl<>(Collections.emptyList(), pageable, 0);
		}
		
		// 기존 처리를 수정하기 전에 우선 count, paging query를 만들어보자.
		
		var countQuery = (PlainSelect) CCJSqlParserUtil.parse(dbQuery.getQueryString());
		var selectQuery = (PlainSelect) CCJSqlParserUtil.parse(dbQuery.getQueryString());
		
		// countQuery column 부분 변경
		{
			countQuery.getSelectItems().clear();
			var function = new Function();
			function.setName("count");
			function.setParameters(new AllColumns());
			countQuery.getSelectItems().add(new SelectItem<Function>(function));
			
			// count query 사용시 지워야하는 조건들이 있을 수 있음 테스트 해보면서 확인해야 할 것 같음.
			
			
		}
		
		var paramSource = new MapSqlParameterSource();
		
		// 추가해야 할 where 조건을 작성해보자.
		{
			// 검색 대상 컬럼 목록 추출
			dbFieldList
				.stream()
				.filter(
						x -> (DbFieldEnable.REQUIRED.equals(x.getEnableSearch()) || DbFieldEnable.ENABLED.equals(x.getEnableSearch()))
						&& dataMap.containsKey(x.getColumnId())
						&& StringUtils.hasText(dataMap.get(x.getColumnId())))
				.forEach(targetField -> {
					// 추가 대상 컬럼에 대해 뭔가 좀더 복잡한 조건이 있을 수 있음.
					

					
					addWhereCondition(countQuery, targetField.getColumnId());
					addWhereCondition(selectQuery, targetField.getColumnId());
					paramSource.addValue(targetField.getColumnId(), dataMap.get(targetField.getColumnId()));
							
				});
			
		}
		
		// countQuery 작성시 기존 쿼리에서 제거해야 하는 항목이 어떤게 있을까...
		// countQuery의 경우 order by 절 제거
		countQuery.setOrderByElements(null);
		
		// selectQuery는 페이징을 위해 limit offset 설정을 추가한다.
		// 만약 limit offset이 쿼리에 등록되어 있어도 해당 설정을 지우고 추가함
		addPagingCondition(selectQuery, pageable.getPageSize(), pageable.getOffset());
		paramSource.addValue("limit", pageable.getPageSize());
		paramSource.addValue("offset", pageable.getOffset());
		

		// 이거 필요한가?
//		dataMap.forEach((key, value) -> paramSource.addValue(key, StringUtils.hasText(value) ? value : null));
		
		String countQueryStr;
		{
			StringBuilder builder = new StringBuilder();
			StatementDeParser deParser = new StatementDeParser(builder);
			deParser.visit(countQuery);
			countQueryStr = countQuery.toString();
		}
		
		String selectQueryStr;
		{
			StringBuilder builder = new StringBuilder();
			StatementDeParser deParser = new StatementDeParser(builder);
			deParser.visit(selectQuery);
			selectQueryStr = selectQuery.toString();
		}
		
		
		// customQuery 조건에 대해 검토 필요
		// 이 부분은 일단 주석 처리함
//		if (SettingStringUtil.isCustomQuery(dbQuery.getQueryString())) {
//			List<Map<String, Object>> contentList = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.query(dbQuery.getQueryString(), paramSource, ROW_MAPPER));
//			var customPageable = PageRequest.of(0, contentList.size() <= 0 ? 1 : contentList.size(), pageable.getSort());
//			return new PageImpl<>(contentList, customPageable, contentList.size());
//		}
		
		// count query를 먼저 조회
		int totalCount = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.queryForObject(countQueryStr, paramSource, Integer.class));
		if (totalCount == 0) {
			return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
		}
		
		List<Map<String, Object>> contentList = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.query(selectQueryStr, paramSource, ROW_MAPPER));
		
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
	
	
	public void addWhereCondition(PlainSelect plainSelect, String columnId) {
		BinaryExpression targetWhereExpression = (BinaryExpression) plainSelect.getWhere();
		
		// 현재는 동등 비교 조건만 추가
		// 이후 기능 확장 가능
		var appendExpression = new EqualsTo(new Column(columnId), new JdbcNamedParameter(columnId));
		
		if (targetWhereExpression == null) {
			plainSelect.setWhere(appendExpression);
			return;
		}
		
		Set<Column> set = new HashSet<>();
		
		// 기존에 해당 컬럼에 대한 조건이 있으면 추가하지 않음
		targetWhereExpression.accept(new ExpressionVisitorAdapter() {
			@Override
			public void visit(Column column) {
				if (appendExpression.getLeftExpression() instanceof Column appendColumn && column.getColumnName().equals(appendColumn.getColumnName())) {
					set.add(column);
				}
			}
		});
		
		plainSelect.setWhere(set.isEmpty() ? new AndExpression(targetWhereExpression, appendExpression) : targetWhereExpression);
		
	}
}

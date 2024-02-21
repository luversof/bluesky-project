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
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.util.SettingStringUtil;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.support.DynamicCrudSettingTransactionHandler;

/**
 * 현재 MySql과 MsSql의 경우 기능이 거의 동일하여 상위 service를 구성함 
 */
public abstract class AbstractDbUseService implements UseService {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private DynamicCrudSettingTransactionHandler dynamicCrudSettingTransactionHandler;
	
	private static final RowMapper<Map<String, Object>> ROW_MAPPER = new ColumnMapRowMapper();
	
	protected abstract String getCountQuery();
	
	protected abstract String getSelectPagingQuery();
	
	protected abstract String getLimitClause();
	
	@Override
	public Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.SELECT);
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		
		RoutingDataSourceContextHolder.setContext(() -> dbQuery.getDataSourceName());
		
		var paramSource = new MapSqlParameterSource();
		
		// 필수 검색 조건이 있는 경우 확인
		var requiredFieldList = dbFieldList.stream().filter(x -> DbFieldEnable.REQUIRED.equals(x.getEnableSearch())).toList();
		if (requiredFieldList.stream().anyMatch(x -> !dataMap.containsKey(x.getColumnId()) || !StringUtils.hasText(dataMap.get(x.getColumnId())))) {
			return new PageImpl<>(Collections.emptyList(), pageable, 0);
		}
		
		// 검색 대상 컬럼 목록 추출
		var whereClauseBuilder = new StringBuilder();
		{
			var targetFieldList = dbFieldList.stream().filter(x -> (DbFieldEnable.REQUIRED.equals(x.getEnableSearch()) || DbFieldEnable.ENABLED.equals(x.getEnableSearch()))
					&& dataMap.containsKey(x.getColumnId())
					&& StringUtils.hasText(dataMap.get(x.getColumnId()))).toList();
			if (!targetFieldList.isEmpty()) {
				boolean checkAlreadyHasWhereClause = false;
				whereClauseBuilder.append("WHERE ");
				
				for (var targetField : targetFieldList) {
					if (checkAlreadyHasWhereClause) {
						whereClauseBuilder.append("AND ");
					}
					whereClauseBuilder.append(String.format("%s = :%s ", targetField.getColumnId(), targetField.getColumnId()));
					paramSource.addValue(targetField.getColumnId(), dataMap.get(targetField.getColumnId()));
					checkAlreadyHasWhereClause = true;
				}
			}
		}
		
		// query String format 매개 변수
		var param = new HashMap<String, String>();
		param.put("tableName", SettingStringUtil.getTableName(dbQuery.getQueryString()));
		param.put("whereClause", whereClauseBuilder.toString());
		param.put("limitClause", getLimitClause());
		
		// mssql은 페이징에 order 절이 필수임
		// orderClause는 우선 등록된 쿼리에 값이 있으면 해당값을 세팅함.
		param.put("orderClause", SettingStringUtil.getOrderClause(dbQuery.getQueryString()));
		// 분기 처리도 해야함
		// limit 절 모양도 다름
		
		
		paramSource.addValue("limit", pageable.getPageSize());
		paramSource.addValue("offset", pageable.getOffset());
		
		dataMap.forEach((key, value) -> paramSource.addValue(key, StringUtils.hasText(value) ? value : null));
		
		
		// select 쿼리의 where 조건까지 작성되어 있는 경우엔 페이징과 검색 조건 생성을 생략하고 바로 처리한다.
		if (SettingStringUtil.isCustomQuery(dbQuery.getQueryString())) {
			List<Map<String, Object>> contentList = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.query(dbQuery.getQueryString(), paramSource, ROW_MAPPER));
			return new PageImpl<>(contentList, pageable, contentList.size());
		}
		
		// count query를 먼저 조회
		int totalCount = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.queryForObject(SettingStringUtil.format(getCountQuery(), param), paramSource, Integer.class));
		if (totalCount == 0) {
			return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
		}
		
		List<Map<String, Object>> contentList = dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.query(SettingStringUtil.format(getSelectPagingQuery(), param), paramSource, ROW_MAPPER));
		
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

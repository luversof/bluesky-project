package net.luversof.web.dynamiccrud.use.service.mariadb;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.support.DynamicCrudSettingTransactionHandler;
import net.luversof.web.dynamiccrud.use.service.UseService;
import net.luversof.web.dynamiccrud.use.util.ThymeleafUseUtil;

@Service
public class MariadbUseService implements UseService {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private DynamicCrudSettingTransactionHandler dynamicCrudSettingTransactionHandler;
	
	private static final RowMapper<Map<String, Object>> ROW_MAPPER = new ColumnMapRowMapper();

	@Override
	public Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap) {
		var dbQuery = ThymeleafUseUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.SELECT);
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(settingParameter);
		
		RoutingDataSourceContextHolder.setContext(() -> dbQuery.getDataSourceName());
		
		var selectQueryBuilder = new StringBuilder(dbQuery.getQueryString());
		
		var countQueryBuilder = new StringBuilder("SELECT COUNT(1) FROM ");
		countQueryBuilder.append(Pattern.compile("FROM", Pattern.CASE_INSENSITIVE).split(dbQuery.getQueryString())[1]);
		
		var paramSource = new MapSqlParameterSource();
		
		// 필수 검색 조건이 있는 경우 확인
		var requiredFieldList = dbFieldList.stream().filter(x -> DbFieldEnable.REQUIRED.equals(x.getEnableSearch())).toList();
		if (requiredFieldList.stream().anyMatch(x -> !dataMap.containsKey(x.getColumnId()))) {
			return new PageImpl<>(Collections.emptyList(), pageable, 0);
		}
		
		// 검색 대상 컬럼 목록 추출
		var conditionQueryBuilder = new StringBuilder();
		var targetFieldList = dbFieldList.stream().filter(x -> (DbFieldEnable.REQUIRED.equals(x.getEnableSearch()) || DbFieldEnable.ENABLED.equals(x.getEnableSearch())) && dataMap.containsKey(x.getColumnId()) && StringUtils.hasText(dataMap.get(x.getColumnId()))).toList();
		if (!targetFieldList.isEmpty()) {
			boolean checkAlreadWhereCondition = false;
			conditionQueryBuilder.append(" WHERE ");
			
			for (var targetField : targetFieldList) {
				if (checkAlreadWhereCondition) {
					conditionQueryBuilder.append("AND ");
				}
				conditionQueryBuilder.append(String.format("%s = :%s ", targetField.getColumnId(), targetField.getColumnId()));
				paramSource.addValue(targetField.getColumnId(), dataMap.get(targetField.getColumnId()));
				checkAlreadWhereCondition = true;
			}
		}
		
		selectQueryBuilder.append(conditionQueryBuilder);
		countQueryBuilder.append(conditionQueryBuilder);
		
		selectQueryBuilder.append(" LIMIT :limit OFFSET :offset");
		paramSource.addValue("limit", pageable.getPageSize());
		paramSource.addValue("offset", pageable.getOffset());
		
		// count query를 먼저 조회
		int totalCount = namedParameterJdbcTemplate.queryForObject(countQueryBuilder.toString(), paramSource, Integer.class);
		if (totalCount == 0) {
			return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
		}
		
		List<Map<String, Object>> contentList = namedParameterJdbcTemplate.query(selectQueryBuilder.toString(), paramSource, ROW_MAPPER);
		
		return new PageImpl<>(contentList, pageable, totalCount);
	}
	
	
	@Override
	public Object create(SettingParameter settingParameter, Map<String, String> dataMap) {
		var dbQuery = ThymeleafUseUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.INSERT);
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(settingParameter);
		return jdbcTemplateUpdate(dbQuery, dbFieldList, dataMap);
	}

	/**
	 * insert/update query는 등록된 쿼리를 그대로 실행하고 넘겨받은 postData만 설정함
	 */
	@Override
	public Object update(SettingParameter settingParameter, Map<String, String> dataMap) {
		var dbQuery = ThymeleafUseUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.UPDATE);
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(settingParameter);
		return jdbcTemplateUpdate(dbQuery, dbFieldList, dataMap);
	}

	
	/**
	 * Delete의 경우 여러 건을 동시에 삭제할 수 있음.
	 * 삭제도 update 쿼리를 통해 수행함
	 */
	@Override
	public Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap) {
		var dbQuery = ThymeleafUseUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.DELETE);
		var dbFieldList = ThymeleafUseUtil.getDbFieldList(settingParameter);
		
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
			System.out.println("test : " + key);
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
	 */
	private Object jdbcTemplateUpdate(DbQuery dbQuery, List<DbField> dbFieldList, Map<String, String> dataMap) {
		RoutingDataSourceContextHolder.setContext(() -> dbQuery.getDataSourceName());
		
		var insertQueryBuilder = new StringBuilder(dbQuery.getQueryString() + " ");
		var paramSource = new MapSqlParameterSource();
		var targetFieldList = dbFieldList.stream().filter(x -> (DbFieldEnable.REQUIRED.equals(x.getEnableSearch()) || DbFieldEnable.ENABLED.equals(x.getEnableSearch())) && dataMap.containsKey(x.getColumnId()) && StringUtils.hasText(dataMap.get(x.getColumnId()))).toList();
		if (!targetFieldList.isEmpty()) {
			setSqlParameterSourceRegisterSqlType(paramSource, dbFieldList);
			dataMap.forEach((key, value) -> paramSource.addValue(key, StringUtils.hasText(value) ? value : null));
		}
		return dynamicCrudSettingTransactionHandler.runInReadUncommittedTransaction(() -> namedParameterJdbcTemplate.update(insertQueryBuilder.toString(), paramSource));
	}
	
	private void setSqlParameterSourceRegisterSqlType(MapSqlParameterSource paramSource, List<DbField> fieldList) {
		fieldList.forEach(field -> {
			if (field.getColumnType().equals(DbFieldColumnType.BOOLEAN)) {
				paramSource.registerSqlType(field.getColumnId(), JDBCType.BIT.getVendorTypeNumber());
			}
		});
	}

}
 
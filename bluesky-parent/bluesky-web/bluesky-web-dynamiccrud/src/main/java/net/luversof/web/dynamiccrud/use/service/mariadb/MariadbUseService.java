package net.luversof.web.dynamiccrud.use.service.mariadb;

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
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.FieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.use.service.UseService;

@Service
public class MariadbUseService implements UseService {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<Map<String, Object>> ROW_MAPPER = new ColumnMapRowMapper();

	@Override
	public Page<Map<String, Object>> find(Query query, List<Field> fieldList, Pageable pageable, Map<String, String> dataMap) {
		RoutingDataSourceContextHolder.setContext(() -> query.getDataSourceName());
		
		var selectQueryBuilder = new StringBuilder(query.getQueryString() + " ");
		
		var countQueryBuilder = new StringBuilder("SELECT COUNT(1) FROM " + query.getQueryString().split("FROM")[1] + " ");
		
		var paramSource = new MapSqlParameterSource();
		
		// 필수 검색 조건이 있는 경우 확인
		var requiredFieldList = fieldList.stream().filter(x -> FieldEnable.REQUIRED.equals(x.getEnableSearch())).toList();
		if (requiredFieldList.stream().anyMatch(x -> !dataMap.containsKey(x.getColumn()))) {
			return new PageImpl<>(Collections.emptyList(), pageable, 0);
		}
		
		// 검색 대상 컬럼 목록 추출
		var conditionQueryBuilder = new StringBuilder();
		var targetFieldList = fieldList.stream().filter(x -> (FieldEnable.REQUIRED.equals(x.getEnableSearch()) || FieldEnable.ENABLED.equals(x.getEnableSearch())) && dataMap.containsKey(x.getColumn()) && StringUtils.hasText(dataMap.get(x.getColumn()))).toList();
		if (!targetFieldList.isEmpty()) {
			boolean checkAlreadWhereCondition = false;
			conditionQueryBuilder.append("WHERE ");
			
			for (var targetField : targetFieldList) {
				if (checkAlreadWhereCondition) {
					conditionQueryBuilder.append("AND ");
				}
				conditionQueryBuilder.append(String.format("%s = :%s ", targetField.getColumn(), targetField.getColumn()));
				paramSource.addValue(targetField.getColumn(), dataMap.get(targetField.getColumn()));
				checkAlreadWhereCondition = true;
			}
		}
		
		selectQueryBuilder.append(conditionQueryBuilder);
		countQueryBuilder.append(conditionQueryBuilder);
		
		selectQueryBuilder.append("LIMIT :limit OFFSET :offset");
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
	
	/**
	 * jdbcTemplate은 insert, update, delete를  update method로 동일하게 수행
	 */
	@Override
	public Object create(Query query, List<Field> fieldList, Map<String, String> dataMap) {
		RoutingDataSourceContextHolder.setContext(() -> query.getDataSourceName());
		
		var insertQueryBuilder = new StringBuilder(query.getQueryString() + " ");
		var paramSource = new MapSqlParameterSource();
		
		var targetFieldList = fieldList.stream().filter(x -> (FieldEnable.REQUIRED.equals(x.getEnableSearch()) || FieldEnable.ENABLED.equals(x.getEnableSearch())) && dataMap.containsKey(x.getColumn()) && StringUtils.hasText(dataMap.get(x.getColumn()))).toList();
		if (!targetFieldList.isEmpty()) {
			dataMap.forEach((key, value) -> paramSource.addValue(key, value));
		}
		
		return namedParameterJdbcTemplate.update(insertQueryBuilder.toString(), paramSource);
	}

	/**
	 * insert/update query는 등록된 쿼리를 그대로 실행하고 넘겨받은 postData만 설정함
	 */
	@Override
	public Object update(Query query, List<Field> fieldList, Map<String, String> dataMap) {
		return create(query, fieldList, dataMap);
	}
	
	
	/**
	 * Delete의 경우 여러 건을 동시에 삭제할 수 있음.
	 * 삭제도 update 쿼리를 통해 수행함
	 */
	@Override
	public Object delete(Query query, List<Field> fieldList, MultiValueMap<String, String> dataMap) {
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
			Object result = update(query, fieldList, map);
			resultList.add(result);
		});
		return resultList;	
	}

}
 
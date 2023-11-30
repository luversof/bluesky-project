package net.luversof.web.dynamiccrud.setting.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.QueryRowMapper;

@Service
public class QueryService implements SettingService<Query> {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<Query> ROW_MAPPER = new QueryRowMapper();

	@Override
	public Page<Query> find(SettingParameter queryParameter, Pageable pageable) {
		
		// select 쿼리 생성
		var selectQueryBuilder = new StringBuilder("SELECT product, mainMenu, subMenu, sqlCommandType, queryString, dataSourceName, dbType, operator, registerDate, modifyDate FROM Queries ");
		var countQueryBuilder = new StringBuilder("SELECT COUNT(1) FROM Queries ");
		
		
		var paramSource = new MapSqlParameterSource();
		
		if (StringUtils.hasText(queryParameter.product())) {
			selectQueryBuilder.append("WHERE product = :product ");
			countQueryBuilder.append("WHERE product = :product ");
			paramSource.addValue("product", queryParameter.product());
		}
		
		if (StringUtils.hasText(queryParameter.mainMenu())) {
			selectQueryBuilder.append("WHERE mainMenu = :mainMenu ");
			countQueryBuilder.append("WHERE mainMenu = :mainMenu ");
			paramSource.addValue("mainMenu", queryParameter.mainMenu());
		}
		
		if (StringUtils.hasText(queryParameter.subMenu())) {
			selectQueryBuilder.append("WHERE subMenu = :subMenu ");
			countQueryBuilder.append("WHERE subMenu = :subMenu ");
			paramSource.addValue("subMenu", queryParameter.subMenu());
		}
		
		selectQueryBuilder.append("LIMIT :limit OFFSET :offset");
		paramSource.addValue("limit", pageable.getPageSize());
		paramSource.addValue("offset", pageable.getOffset());
		
		// count query를 먼저 조회
		int totalCount = namedParameterJdbcTemplate.queryForObject(countQueryBuilder.toString(), paramSource, Integer.class);
		if (totalCount == 0) {
			return new PageImpl<>(Collections.emptyList(), pageable, totalCount);	
		}
		
		
		List<Query> queryList = namedParameterJdbcTemplate.query(selectQueryBuilder.toString(), paramSource, ROW_MAPPER);
		
		return new PageImpl<>(queryList, pageable, totalCount);
	}

}

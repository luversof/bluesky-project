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

import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.MainMenuRowMapper;

@Service
public class MainMenuService implements SettingService<MainMenu> {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<MainMenu> ROW_MAPPER = new MainMenuRowMapper();

	@Override
	public Page<MainMenu> find(SettingParameter queryParameter, Pageable pageable) {
		
		// select 쿼리 생성
		var selectQueryBuilder = new StringBuilder("SELECT product, mainMenu, mainMenuName, operator, registerDate, modifyDate FROM MainMenus ");
		var countQueryBuilder = new StringBuilder("SELECT COUNT(1) FROM MainMenus ");
		
		
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
		
		selectQueryBuilder.append("LIMIT :limit OFFSET :offset");
		paramSource.addValue("limit", pageable.getPageSize());
		paramSource.addValue("offset", pageable.getOffset());
		
		// count query를 먼저 조회
		int totalCount = namedParameterJdbcTemplate.queryForObject(countQueryBuilder.toString(), paramSource, Integer.class);
		if (totalCount == 0) {
			return new PageImpl<>(Collections.emptyList(), pageable, totalCount);	
		}
		
		
		List<MainMenu> mainMenuList = namedParameterJdbcTemplate.query(selectQueryBuilder.toString(), paramSource, ROW_MAPPER);
		
		return new PageImpl<>(mainMenuList, pageable, totalCount);
	}

}

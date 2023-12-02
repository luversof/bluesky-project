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
import net.luversof.web.dynamiccrud.setting.repository.MainMenuRepository;

@Service
public class MainMenuService implements SettingService<MainMenu> {
	
	@Autowired
	private MainMenuRepository mainMenuRepository;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private SettingDataService settingDataService;
	
	private static final RowMapper<MainMenu> ROW_MAPPER = new MainMenuRowMapper();

	@Override
	public Page<MainMenu> find(SettingParameter settingParameter, Pageable pageable) {
		
		// select 쿼리 생성
		var selectQueryBuilder = new StringBuilder("SELECT product, mainMenu, mainMenuName, operator, registerDate, modifyDate FROM MainMenus ");
		var countQueryBuilder = new StringBuilder("SELECT COUNT(1) FROM MainMenus ");
		
		
		var paramSource = new MapSqlParameterSource();
		
		if (StringUtils.hasText(settingParameter.product())) {
			selectQueryBuilder.append("WHERE product = :product ");
			countQueryBuilder.append("WHERE product = :product ");
			paramSource.addValue("product", settingParameter.product());
		}
		
		if (StringUtils.hasText(settingParameter.mainMenu())) {
			selectQueryBuilder.append("WHERE mainMenu = :mainMenu ");
			countQueryBuilder.append("WHERE mainMenu = :mainMenu ");
			paramSource.addValue("mainMenu", settingParameter.mainMenu());
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
	
	public MainMenu findByProductAndMainMenu(String product, String mainMenu) {
		var targetMainMenu = settingDataService.getMainMenuList().stream().filter(subMenu -> subMenu.getProduct().equals(product) && subMenu.getMainMenu().equals(mainMenu)).findAny().orElseGet(() -> null);
		if (targetMainMenu == null) {
			targetMainMenu = mainMenuRepository.findByProductAndMainMenu(product, mainMenu);
		}
		return targetMainMenu;
	}

}

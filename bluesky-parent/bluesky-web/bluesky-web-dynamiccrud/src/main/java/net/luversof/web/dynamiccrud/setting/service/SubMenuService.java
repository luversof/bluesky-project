package net.luversof.web.dynamiccrud.setting.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.SubMenuRowMapper;
import net.luversof.web.dynamiccrud.setting.repository.SubMenuRepository;

@Service
public class SubMenuService implements SettingService<SubMenu> {
	
	@Autowired
	private SubMenuRepository subMenuRepository;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private SettingDataService settingDataService;
	
	private static final RowMapper<SubMenu> ROW_MAPPER = new SubMenuRowMapper();

	@Override
	public Page<SubMenu> find(SettingParameter settingParameter, Pageable pageable) {
		
		// select 쿼리 생성
		var selectQueryBuilder = new StringBuilder("SELECT product, mainMenu, subMenu, subMenuName, template, displayOrder, groupNo, groupTemplate, pageSize, enableCount, enableExcel, enableInsert, enableUpdate, enableDelete, operator, registerDate, modifyDate FROM SubMenus ");
		var countQueryBuilder = new StringBuilder("SELECT COUNT(1) FROM SubMenus ");
		
		
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
		
		if (StringUtils.hasText(settingParameter.subMenu())) {
			selectQueryBuilder.append("WHERE subMenu = :subMenu ");
			countQueryBuilder.append("WHERE subMenu = :subMenu ");
			paramSource.addValue("subMenu", settingParameter.subMenu());
		}
		
		selectQueryBuilder.append("LIMIT :limit OFFSET :offset");
		paramSource.addValue("limit", pageable.getPageSize());
		paramSource.addValue("offset", pageable.getOffset());
		
		// count query를 먼저 조회
		int totalCount = namedParameterJdbcTemplate.queryForObject(countQueryBuilder.toString(), paramSource, Integer.class);
		if (totalCount == 0) {
			return new PageImpl<>(Collections.emptyList(), pageable, totalCount);	
		}
		
		
		List<SubMenu> subMenuList = namedParameterJdbcTemplate.query(selectQueryBuilder.toString(), paramSource, ROW_MAPPER);
		
		return new PageImpl<>(subMenuList, pageable, totalCount);
	}
	
	public List<SubMenu> findByProductAndMainMenu(String product, String mainMenu) {
		var subMenuList = settingDataService.getSubMenuList().stream().filter(subMenu -> subMenu.getProduct().equals(product) && subMenu.getMainMenu().equals(mainMenu)).collect(Collectors.toList());
		if (subMenuList.isEmpty()) {
			subMenuList = subMenuRepository.findByProductAndMainMenu(product, mainMenu);
		}
		return subMenuList ;
	}

}

package net.luversof.web.dynamiccrud.setting.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.Setting;
import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.repository.FieldRepository;
import net.luversof.web.dynamiccrud.setting.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud.setting.repository.ProductRepository;
import net.luversof.web.dynamiccrud.setting.repository.QueryRepository;
import net.luversof.web.dynamiccrud.setting.repository.SubMenuRepository;


/**
 * 아래 find는 이후 jdbcClient로 교체 여부 검토 예정
 */
@Service
public class SettingServiceTemp {
	
//	@Autowired
//	private Map<String, SettingRepository<SettingClass, ?>> settingRepositoryMap;
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private MainMenuRepository mainMenuRepository;
	
	@Autowired
	private SubMenuRepository subMenuRepository;
	
	@Autowired
	private QueryRepository queryRepository;
	
	@Autowired
	private FieldRepository fieldRepository;

	@SuppressWarnings("unchecked")
	public <T extends Setting> Page<T> find(String type, SettingParameter queryParameter, Pageable pageable) {
//		var repository = getRepository(type);
		
		// 이거 조건별로 체크하는거 어케 하면 좋을까?
		// 어떻게 해야 할까?
		if ("product".equals(type)) {
			return (Page<T>) findProduct(type, queryParameter, pageable);
		} else if ("mainMenu".equals(type)) {
			return (Page<T>) findMainMenu(type, queryParameter, pageable);
		} else if ("subMenu".equals(type)) {
			return (Page<T>) findSubMenu(type, queryParameter, pageable);
		} else if ("query".equals(type)) {
			return (Page<T>) findQuery(type, queryParameter, pageable);
		} else if ("field".equals(type)) {
			return (Page<T>) findField(type, queryParameter, pageable);
		}
		
		return null;
	}
	
	private Page<Product> findProduct(String type, SettingParameter queryParameter, Pageable pageable) {
		if (!StringUtils.hasText(queryParameter.product())) {
			return productRepository.findAll(pageable);
		} else {
			return productRepository.findByProduct(queryParameter.product(), pageable);
		}
	}
	
	private Page<MainMenu> findMainMenu(String type, SettingParameter queryParameter, Pageable pageable) {
		var hasProduct = StringUtils.hasText(queryParameter.product());
		var hasMainMenu = StringUtils.hasText(queryParameter.mainMenu());
		
		if (!hasProduct && !hasMainMenu) {
			return mainMenuRepository.findAll(pageable);
		} else if (hasProduct && hasMainMenu) {
			return mainMenuRepository.findByProductAndMainMenu(queryParameter.product(), queryParameter.mainMenu(), pageable);
		} else if (!hasProduct && hasMainMenu) {
			return mainMenuRepository.findByMainMenu(queryParameter.mainMenu(), pageable);
		} else if (hasProduct && !hasMainMenu) {
			return mainMenuRepository.findByProduct(queryParameter.product(), pageable);
		}
		throw new BlueskyException("여기까지 올 일 없음");
	}

	
	private Page<SubMenu> findSubMenu(String type, SettingParameter queryParameter, Pageable pageable) {
		return subMenuRepository.findAll(pageable);
	}
	
	private Page<Query> findQuery(String type, SettingParameter queryParameter, Pageable pageable) {
		return queryRepository.findAll(pageable);
	}
	
	private Page<Field> findField(String type, SettingParameter queryParameter, Pageable pageable) {
		return fieldRepository.findAll(pageable);
	}
	
	
//	private SettingRepository<SettingClass, ?> getRepository(String type) {
//		return settingRepositoryMap.get(type + "Repository");
//	}

}

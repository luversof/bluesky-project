package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "MainMenus")
@IdClass(MainMenuId.class)
public class MainMenu extends SettingClass {
	
	@Id
	@Column(length = 20)
	private String product;
	
	@Id
	@Column(length = 40)
	private String mainMenu;
	
	private String mainMenuName;

}

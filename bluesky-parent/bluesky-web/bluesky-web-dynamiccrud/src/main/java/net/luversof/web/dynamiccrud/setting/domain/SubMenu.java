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
@Table(name = "SubMenus")
@IdClass(SubMenuId.class)
public class SubMenu extends Setting {
	
	@Id
	@Column(length = 20)
	private String product;
	
	@Id
	@Column(length = 40)
	private String mainMenu;
	
	@Id
	@Column(length = 40)
	private String subMenu;
	
	private String subMenuName;
	
	private String template;
	
	private int displayOrder;
	
	private int groupNo;
	
	private String groupTemplate;
	
	private int pageSize;
	
	private boolean enableCount;
	
	private boolean enableExcel;
	
	private boolean enableInsert;
	
	private boolean enableUpdate;
	
	private boolean enableDelete;

}

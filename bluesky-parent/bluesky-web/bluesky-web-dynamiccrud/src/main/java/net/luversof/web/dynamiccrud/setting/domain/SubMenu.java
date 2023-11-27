package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class SubMenu extends AbstractSettingClass {
	
	private String subMenuName;

}

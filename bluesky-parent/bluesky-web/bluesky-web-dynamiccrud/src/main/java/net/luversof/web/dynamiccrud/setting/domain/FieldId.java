package net.luversof.web.dynamiccrud.setting.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldId implements SettingClassId {

	private static final long serialVersionUID = 1L;

	private String product;
	
	private String mainMenu;
	
	private String subMenu;
	
	private String column;

}

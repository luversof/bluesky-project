package net.luversof.web.dynamiccrud.setting.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainMenuId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String product;
	
	private String mainMenu;
}

package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Queries")
@IdClass(QueryId.class)
public class Query extends Setting {
	
	@Id
	@Column(length = 20)
	private String product;
	
	@Id
	@Column(length = 40)
	private String mainMenu;
	
	@Id
	@Column(length = 40)
	private String subMenu;
	
	@Id
	@Column(length = 20)
	private String sqlCommandType;	// INSERT, SELECT, UPDATE, DELETE
	
	private String dataSourceName;
	
	private String dbType;	//MsSql, MySql
	
	private String queryString;

}

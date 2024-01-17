package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DbQuery", uniqueConstraints = @UniqueConstraint(columnNames = { "adminProjectId", "projectId", "mainMenuId", "subMenuId", "sqlCommandType" }))
public class DbQuery extends Setting {
	
	@Column(length = 20)
	private String adminProjectId;
	
	@Column(length = 20)
	private String projectId;
	
	@Column(length = 40)
	private String mainMenuId;
	
	@Column(length = 40)
	private String subMenuId;
	
	@Column(length = 20)
	@Enumerated(EnumType.STRING)
	private DbQuerySqlCommandType sqlCommandType;	// INSERT, SELECT, UPDATE, DELETE
	
	@Column(nullable = false, columnDefinition = "TEXT")
	private String queryString;
	
	@Column(length = 40, nullable = false)
	private String dataSourceName;

}

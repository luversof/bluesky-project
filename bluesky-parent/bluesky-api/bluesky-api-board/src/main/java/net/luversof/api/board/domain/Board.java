package net.luversof.api.board.domain;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Table("Board")
public class Board {

	@Id
	@Column("id")
	@Null(groups = Create.class)
	@NotNull(groups = Update.class)
	private UUID id;
	
	@Column("alias")
	private String alias;
	
	@Column("jsonConfig")
	private Map<String, Object> jsonConfig;
	
	public interface Create {}
	public interface Update {}

}

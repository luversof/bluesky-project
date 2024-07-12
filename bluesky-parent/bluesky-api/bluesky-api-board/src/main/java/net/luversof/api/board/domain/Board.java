package net.luversof.api.board.domain;

import java.util.BitSet;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_board_alias", columnList = "alias", unique = true) })
public class Board {

	@Null(groups = Create.class)
	@NotNull(groups = Update.class)
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(length = 15, nullable = false)
	private String alias;
	
	private BitSet bitConfig;
	
	public interface Create {}
	public interface Update {}
	
}

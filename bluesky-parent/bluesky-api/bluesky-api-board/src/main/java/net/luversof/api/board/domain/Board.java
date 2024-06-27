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
@Table(indexes = { @Index(name = "UK_board_boardId", columnList = "boardId", unique = true), @Index(name = "UK_board_alias", columnList = "alias", unique = true) })
public class Board {

	@Null(groups = Create.class)
	@NotNull(groups = Update.class)
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(length = 15, nullable = false)
	private String alias;
	
	private BitSet config;
	
	// 속성 설정 값이 너무 많으면 어떻게 관리하는게 좋을까?
	// 그룹으로 묶는 것이 좋을까?
	
	private boolean isBoardActivated;
	
	private boolean isArticleActivated;
	
	private boolean isReplyActivated;
	
	private boolean isCommentActivated;
	
	public interface Create {}
	public interface Update {}
	
}

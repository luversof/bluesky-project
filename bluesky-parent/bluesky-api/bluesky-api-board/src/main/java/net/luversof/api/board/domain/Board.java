package net.luversof.api.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_board_boardId", columnList = "boardId", unique = true), @Index(name = "UK_board_alias", columnList = "alias", unique = true) })
public class Board {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(length = 36, nullable = false)
	private String boardId;
	
	@Column(length = 15, nullable = false)
	private String alias;
	
	// 속성 설정 값이 너무 많으면 어떻게 관리하는게 좋을까?
	// 그룹으로 묶는 것이 좋을까?
	
	private boolean isBoardActivated;
	
	private boolean isArticleActivated;
	
	private boolean isReplyActivated;
	
	private boolean isCommentActivated;
	
}

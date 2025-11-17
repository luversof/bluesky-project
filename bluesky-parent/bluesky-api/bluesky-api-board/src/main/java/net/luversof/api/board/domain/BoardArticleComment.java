package net.luversof.api.board.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Table("BoardArticleComment")
public class BoardArticleComment {

	@Id
	@Column("id")
	@Null(groups = Create.class)
	@NotNull(groups = { Modify.class, Delete.class })
	private UUID id;

	@NotNull(groups = { Create.class })
	@Column("board_article_id")
	private UUID boardArticleId;

	@NotNull(groups = { Create.class, Modify.class, Delete.class })
	@Column("user_id")
	private UUID userId;

	@NotBlank(groups = { Create.class, Modify.class })
	@Column("content")
	private String content;

	@CreatedDate
	@Column("createdDate")
	private Instant createdDate;

	@LastModifiedDate
	@Column("lastModifiedDate")
	private Instant lastModifiedDate;

	public interface Create {
	}

	public interface Modify {
	}

	public interface Delete {
	}
}

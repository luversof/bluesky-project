package net.luversof.api.board.domain;

import java.time.OffsetDateTime;
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
@Table(name = "BoardArticle")
public class BoardArticle {

	@Id
	@Null(groups = Create.class)
	@NotNull(groups = { Get.class })
	@Column("id")
	private UUID id;

	@NotBlank(groups = { Create.class, Modify.class, Delete.class })
	@Column("user_id")
	private String userId;

	@Column("board_id")
	private UUID boardId;

	@NotBlank(groups = { Create.class, Modify.class })
	@Column("title")
	private String title;

	@NotBlank(groups = { Create.class, Modify.class })
	@Column("content")
	private String content;

	@CreatedDate
	@Column("createdDate")
	private OffsetDateTime createdDate;

	@LastModifiedDate
	@Column("lastModifiedDate")
	private OffsetDateTime lastModifiedDate;
	
	public interface Create {}

	public interface Get {}

	public interface Modify {}
	
	public interface Delete {}
}
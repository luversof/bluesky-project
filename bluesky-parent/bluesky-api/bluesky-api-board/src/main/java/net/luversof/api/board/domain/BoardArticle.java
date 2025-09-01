package net.luversof.api.board.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;
import net.luversof.api.board.annotation.CreatedDate;
import net.luversof.api.board.annotation.LastModifiedDate;

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
	private ZonedDateTime createdDate;

	@LastModifiedDate
	@Column("lastModifiedDate")
	private ZonedDateTime lastModifiedDate;
	
	public interface Create {}

	public interface Get {}

	public interface Modify {}
	
	public interface Delete {}
}
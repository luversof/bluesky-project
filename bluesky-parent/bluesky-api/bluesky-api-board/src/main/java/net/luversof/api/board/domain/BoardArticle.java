package net.luversof.api.board.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
public class BoardArticle {

	@Id
	@Null(groups = Create.class)
	@NotNull(groups = { Get.class })
	private UUID id;

	@NotBlank(groups = { Create.class, Modify.class, Delete.class })
	private String userId;

	private UUID boardId;

	@NotBlank(groups = { Create.class, Modify.class })
	private String title;

	@NotBlank(groups = { Create.class, Modify.class })
	private String content;

	@CreatedDate
	private ZonedDateTime createdDate;

	@LastModifiedDate
	private ZonedDateTime lastModifiedDate;
	
	public interface Create {}

	public interface Get {}

	public interface Modify {}
	
	public interface Delete {}
}
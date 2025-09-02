package net.luversof.api.bookkeeping.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Table("Bookkeeping")
public class Bookkeeping {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	private UUID id;
	
	
	@NotNull(groups = { Create.class, Update.class, Delete.class })
	@Column("user_id")
	private UUID userId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	@CreatedDate
	@Column("createdDate")
	private OffsetDateTime createDate;
	
	private BookeepingExtraData extraData = new BookeepingExtraData();
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}
	public interface Search {}
	
	
	@Data
	public static class BookeepingExtraData {
		
		/**
		 * 시작일. startDay라고 해야하나?
		 * 주차 기준을 희망할 경우 설정을 고민해보아야 할듯?
		 */
		@Min(value = 1, groups = { Create.class, Update.class })
		@Max(value = 28, groups = { Create.class, Update.class })
		private int baseDate = 1;
	
	}

}

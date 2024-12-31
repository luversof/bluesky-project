package net.luversof.api.bookkeeping.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.github.f4b6a3.uuid.alt.GUID;

import io.github.luversof.boot.context.ApplicationContextUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Entity
@Table(name = "Bookkeeping", indexes = { @Index(name = "IX_bookkeeping", columnList = "user_id") })
public class Bookkeeping {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	private UUID id;
	
	
	@NotNull(groups = { Create.class, Update.class, Delete.class })
	@Column(name = "user_id", nullable = false)
	private UUID userId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	@CreationTimestamp
	private ZonedDateTime createDate;
	
	@JdbcTypeCode(SqlTypes.JSON)
	private BookeepingExtraData extraData = new BookeepingExtraData();
	
	
	@PrePersist
	public void prePersist() {
		if (ApplicationContextUtil.getApplicationContext().getEnvironment().matchesProfiles("localdev") && id != null) {
			return;
		}
		id = GUID.v7().toUUID();
	}

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

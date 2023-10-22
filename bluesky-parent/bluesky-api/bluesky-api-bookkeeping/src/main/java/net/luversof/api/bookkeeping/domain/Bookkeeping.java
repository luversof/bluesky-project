package net.luversof.api.bookkeeping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_bookkeeping_bookkeepingId", columnList = "bookkeepingId", unique = true), @Index(name = "IDX_bookkeeping_userId", columnList = "user_id") })
public class Bookkeeping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = { Update.class, Delete.class })
	@Column(length = 36, nullable = false)
	private String bookkeepingId;
	
	@NotBlank(groups = { Create.class, Update.class, Delete.class })
	@Column(name = "user_id", length = 16)
	private String userId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	/**
	 * 시작일. startDay라고 해야하나?
	 */
	@Min(value = 1, groups = { Create.class, Update.class })
	@Max(value = 28, groups = { Create.class, Update.class })
	private int baseDate = 1;

	public interface Create {
	}

	public interface Update {
	}
	
	public interface Delete {
	}

	public interface Search {
	}
}

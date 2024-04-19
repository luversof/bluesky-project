package net.luversof.api.bookkeeping.base.domain;

import java.util.UUID;

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
@Table(indexes = { @Index(name = "IDX_bookkeeping_userId", columnList = "user_id") })
public class Bookkeeping {

	@NotBlank(groups = { Update.class, Delete.class })
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	
	@NotBlank(groups = { Create.class, Update.class, Delete.class })
	@Column(name = "user_id", length = 16)
	private UUID userId;
	
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

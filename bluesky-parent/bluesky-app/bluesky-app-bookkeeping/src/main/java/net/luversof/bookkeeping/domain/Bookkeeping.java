package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Range;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_Bookkeeping_userId", columnList = "user_id", unique = true))
public class Bookkeeping {

	@Id
	@GeneratedValue(generator = "uuid-gen")
	@GenericGenerator(name = "uuid-gen", strategy = "uuid2")
	@Column(length = 16)
	private long idx;
	
	@NotBlank(groups = { Create.class, Update.class, Delete.class })
	@Column(length = 36, nullable = false, unique = true)
	private String bookkeepingId;
	
	@NotBlank(groups = { Create.class, Update.class, Delete.class })
	@Column(name = "user_id", length = 16)
	private String userId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	/**
	 * 시작일. startDay라고 해야하나?
	 */
	@Range(min = 1, max = 28, groups = { Create.class, Update.class })
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

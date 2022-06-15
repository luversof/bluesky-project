package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;
import net.luversof.bookkeeping.constant.EntryGroupType;

/**
 * 분류 항목
 * 
 * @author bluesky
 *
 */
@Entity
@Data
public class EntryGroup {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;
	
	@Column(length = 36, nullable = false, unique = true)
	private String entryGroupId;

	@Column(name = "bookkeeping_id", length = 36, nullable = false)
	private String bookkeepingId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;
	
	@NotNull(groups = { Create.class, Update.class })
	@Enumerated(EnumType.STRING)
	private EntryGroupType entryGroupType;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

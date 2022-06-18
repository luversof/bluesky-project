package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
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
@Data
@Entity
@Table(indexes = { @Index(name = "UK_entryGroup_entryGroupId", columnList = "entryGroupId", unique = true), @Index(name = "IDX_entryGroup_bookkeepingId", columnList = "bookkeeping_id") })
public class EntryGroup {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long idx;
	
	@Column(length = 36, nullable = false)
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

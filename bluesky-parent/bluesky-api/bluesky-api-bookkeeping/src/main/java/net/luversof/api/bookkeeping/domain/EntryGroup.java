package net.luversof.api.bookkeeping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import net.luversof.api.bookkeeping.constant.EntryGroupType;

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
	private long idx;
	
	@NotBlank(groups = { Update.class, Delete.class })
	@Column(length = 36, nullable = false)
	private String entryGroupId;

	@Column(name = "bookkeeping_id", length = 36, nullable = false)
	private String bookkeepingId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;
	
	@NotBlank(groups = { Create.class, Update.class })
	@Enumerated(EnumType.STRING)
	private EntryGroupType entryGroupType;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

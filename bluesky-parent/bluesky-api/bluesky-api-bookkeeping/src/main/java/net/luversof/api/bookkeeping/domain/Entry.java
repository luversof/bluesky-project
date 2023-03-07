package net.luversof.api.bookkeeping.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import net.luversof.api.bookkeeping.constant.EntryGroupType;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_entry_entryId", columnList = "entryId", unique = true), @Index(name = "IDX_entry_bookkeepingId", columnList = "bookkeeping_id") })
public class Entry implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = { Update.class, Delete.class })
	@Column(length = 36, nullable = false)
	private String entryId;

	@Column(name = "bookkeeping_id", length = 36, nullable = false)
	private String bookkeepingId;
	
	@NotBlank(groups = { Create.class })
	@Enumerated(EnumType.STRING)
	private EntryGroupType entryGroupType;

	@Column(name = "incomeAsset_id", length = 36)
	private String incomeAssetId;

	@Column(name = "expenseAsset_id", length = 36)
	private String expenseAssetId;

	@Column(name = "entryGroup_id", length = 36, nullable = false)
	private String entryGroupId;

	@Min(value = 1, groups = { Create.class, Update.class })
	private long amount;

	@NotBlank(groups = { Create.class, Update.class })
	private ZonedDateTime entryDate;

	private String memo;

	public interface Create {
	}

    public interface Update {
	}

    public interface Delete {
	}

}

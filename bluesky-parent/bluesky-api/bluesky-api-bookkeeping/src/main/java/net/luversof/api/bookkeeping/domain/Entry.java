package net.luversof.api.bookkeeping.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

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

	@NotBlank(groups = { Update.class, Delete.class })
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@NotBlank(groups = { Create.class })
	@Enumerated(EnumType.STRING)
	private EntryGroupType entryGroupType;

	@Column(name = "incomeAsset_id", length = 36)
	private UUID incomeAssetId;

	@Column(name = "expenseAsset_id", length = 36)
	private UUID expenseAssetId;

	@Column(name = "entryGroup_id", length = 36, nullable = false)
	private UUID entryGroupId;

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

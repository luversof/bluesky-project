package net.luversof.bookkeeping.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Entity
@Data
public class Entry implements Serializable {
	private static final long serialVersionUID = -5106564257765676653L;

	@Id
	@GeneratedValue
	@NotNull(groups = {Update.class, Delete.class})
	@Min(value = 1, groups = {Update.class, Delete.class})
	private long id;
	
	@OneToOne
	@Valid
	private Bookkeeping bookkeeping;
	
	@OneToOne
	@JoinColumn(name = "debit_asset_id")
	private Asset debitAsset;
	
	@OneToOne
	@JoinColumn(name = "credit_asset_id")
	private Asset creditAsset;
	
	@OneToOne
	private EntryGroup entryGroup;
	
	@NotNull(groups = { Create.class, Update.class })
	private long amount;
	
	@NotNull(groups = { Create.class, Update.class })
	private LocalDateTime entryDate;
	
	private String memo;
	
	public interface Create {
	};
	
	public interface Update {
	};
	
	public interface Delete {
	}
	
	public EntryType getEntryType() {
		if (this.debitAsset == null && this.creditAsset != null) {
			return EntryType.CREDIT;
		} else if (this.debitAsset != null && this.creditAsset == null) {
			return EntryType.DEBIT;
		} else if (this.debitAsset != null && this.creditAsset != null) {
			return EntryType.TRANSFER;
		}
		return null;
	}
}

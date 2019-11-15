package net.luversof.bookkeeping.domain;

import java.io.Serializable;
import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import net.luversof.bookkeeping.constant.EntryGroupType;

@Entity
@Data
public class Entry implements Serializable {
	private static final long serialVersionUID = -5106564257765676653L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Update.class, Delete.class })
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;

	@ManyToOne
	private Bookkeeping bookkeeping;
	
	@NotNull(groups = { Create.class })
	@Enumerated(EnumType.STRING)
	private EntryGroupType entryGroupType;

	@ManyToOne
	@JoinColumn(name = "incomeAsset_id")
	private Asset incomeAsset;

	@ManyToOne
	@JoinColumn(name = "expenseAsset_id")
	private Asset expenseAsset;

	@ManyToOne
	private EntryGroup entryGroup;

	@Min(value = 1, groups = { Create.class, Update.class })
	private long amount;

	@NotNull(groups = { Create.class, Update.class })
	private LocalDate entryDate;

	private String memo;

	public interface Create {
	}

    public interface Update {
	}

    public interface Delete {
	}

}

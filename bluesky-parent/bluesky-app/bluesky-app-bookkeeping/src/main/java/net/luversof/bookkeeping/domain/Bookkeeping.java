package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_Bookkeeping_userId", columnList = "user_id"))
public class Bookkeeping {

	@Id
	@GeneratedValue
	@Min(value = 1, groups = { Bookkeeping.Update.class, Bookkeeping.Delete.class, 
			Asset.Create.class, Asset.Update.class, Asset.Delete.class, 
			EntryGroup.Create.class, EntryGroup.Update.class, EntryGroup.Delete.class,
			Entry.Create.class, Entry.Update.class, Entry.Delete.class,
			EntrySearchInfo.Select.class, EntrySearchInfo.SelectEntryList.class, 
			StatisticsSearchInfo.Select.class, StatisticsSearchInfo.SelectEntryList.class 
			})
	private long id;

	@NotEmpty(groups = { Create.class, Update.class })
	private String name;

	@Column(name = "user_id", updatable = false)
	@Min(value = 1, groups = Update.class)
	private long userId;

	/**
	 * 시작일. startDay라고 해야하나?
	 */
	@Range(min = 1, max = 28, groups = { Create.class, Update.class })
	private int baseDate;

	public interface Create {
	};

	public interface Update {
	};

	public interface Delete {
	}
}

package net.luversof.bookkeeping.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Min;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.NotEmpty;
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
	@Min.List({
		@Min(value = 1, groups = { Bookkeeping.Update.class, Bookkeeping.Delete.class }),
		@Min(value = 1, groups = { Asset.Create.class, Asset.Update.class, Asset.Delete.class }),
		@Min(value = 1, groups = { EntryGroup.Create.class, EntryGroup.Update.class, EntryGroup.Delete.class }),
		@Min(value = 1, groups = { Entry.Create.class, Entry.Update.class, Entry.Delete.class }),
		@Min(value = 1, groups = { EntrySearchInfo.Select.class, EntrySearchInfo.SelectEntryList.class }),
		@Min(value = 1, groups = { StatisticsSearchInfo.Select.class, StatisticsSearchInfo.SelectEntryList.class })
	})
	private UUID id;

	@NotEmpty(groups = { Create.class, Update.class })
	private String name;

	@Column(name = "user_id", length = 16)
	@NotEmpty(groups = Update.class)
	private UUID userId;

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

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
import net.luversof.bookkeeping.domain.Asset.AssetDelete;
import net.luversof.bookkeeping.domain.EntryGroup.EntryGroupDelete;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_Bookkeeping_userId", columnList = "user_id"))
public class Bookkeeping {

	@Id
	@GeneratedValue
	@Min(value = 1, groups = { BookkeepingUpdate.class, BookkeepingDelete.class, AssetDelete.class, EntryGroupDelete.class })
	private long id;

	@NotEmpty(groups = { BookkeepingCreate.class, BookkeepingUpdate.class })
	private String name;

	@Column(name = "user_id", updatable = false)
	@Min(value = 1, groups = BookkeepingUpdate.class)
	private long userId;

	/**
	 * 시작일. startDay라고 해야하나?
	 */
	@Range(min = 1, max = 28, groups = { BookkeepingCreate.class, BookkeepingUpdate.class })
	private int baseDate;

	public interface BookkeepingCreate {
	};

	public interface BookkeepingUpdate {
	};

	public interface BookkeepingDelete {
	}
}

package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

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
	@GeneratedValue
	@NotNull(groups = EntryGroupUpdate.class)
	@Min(value = 1, groups = { EntryGroupUpdate.class, EntryGroupDelete.class })
	private long id;

	@NotEmpty(groups = { EntryGroupCreate.class, EntryGroupUpdate.class })
	private String name;

	@NotNull(groups = { EntryGroupCreate.class, EntryGroupUpdate.class })
	@Enumerated(EnumType.STRING)
	private EntryType entryType;

	@OneToOne
	@Valid
	private Bookkeeping bookkeeping;

	public interface EntryGroupCreate {
	}

	public interface EntryGroupUpdate {
	}

	public interface EntryGroupDelete {
	}
}

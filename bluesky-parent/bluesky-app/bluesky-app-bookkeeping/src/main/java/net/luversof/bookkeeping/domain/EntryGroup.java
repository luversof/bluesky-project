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
	@NotNull(groups = Update.class)
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;

	@NotEmpty(groups = { Create.class, Update.class })
	private String name;

	@NotNull(groups = { Create.class, Update.class })
	@Enumerated(EnumType.STRING)
	private EntryType entryType;

	@OneToOne
	@Valid
	private Bookkeeping bookkeeping;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

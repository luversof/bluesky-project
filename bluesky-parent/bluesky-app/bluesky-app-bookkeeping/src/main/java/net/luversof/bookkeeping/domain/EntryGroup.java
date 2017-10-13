package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import lombok.Data;
import net.luversof.bookkeeping.constant.EntryType;

/**
 * 분류 항목
 * 
 * @author bluesky
 *
 */
@Entity
@Data
@Audited
public class EntryGroup {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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

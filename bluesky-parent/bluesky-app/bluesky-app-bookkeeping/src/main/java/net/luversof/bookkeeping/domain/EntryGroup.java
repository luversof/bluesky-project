package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import lombok.Data;
import net.luversof.bookkeeping.constant.EntryGroupType;

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
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;

	@NotEmpty(groups = { Create.class, Update.class })
	private String name;

	@ManyToOne
	private Bookkeeping bookkeeping;
	
	@NotNull(groups = { Create.class, Update.class })
	@Enumerated(EnumType.STRING)
	private EntryGroupType entryType;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

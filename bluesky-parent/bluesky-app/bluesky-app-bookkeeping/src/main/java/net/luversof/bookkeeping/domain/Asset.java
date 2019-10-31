package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
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

/**
 * 자산
 * 
 * @author bluesky
 *
 */
@Entity
@Data
@Audited
public class Asset {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Update.class, Delete.class })
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;

	@NotEmpty(groups = { Create.class, Update.class })
	private String name;

	private long amount;

	// @JsonIgnore
	@OneToOne
	@Valid
	private Bookkeeping bookkeeping;

	@OneToOne
	@NotNull(groups = { Create.class, Update.class })
	private AssetGroup assetGroup;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

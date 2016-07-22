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
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

/**
 * 자산
 * 
 * @author bluesky
 *
 */
@Entity
@Data
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

	@NotNull(groups = { Create.class, Update.class })
	@Enumerated(EnumType.STRING)
	private AssetType assetType;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

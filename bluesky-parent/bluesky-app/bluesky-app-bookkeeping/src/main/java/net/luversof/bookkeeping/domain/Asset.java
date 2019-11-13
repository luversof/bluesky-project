package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

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
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;

	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	private long amount;

	// @JsonIgnore
	@OneToOne
	@Valid
	private Bookkeeping bookkeeping;

	@ManyToOne
	/* @NotNull(groups = { Create.class, Update.class }) */
	@Valid
	private AssetGroup assetGroup;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

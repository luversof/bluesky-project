package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

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
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;

	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	private long amount;

	@Column(name = "bookkeeping_id", length = 36, nullable = false)
	private String bookkeepingId;

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

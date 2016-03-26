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
 * 자산
 * 
 * @author bluesky
 *
 */
@Entity
@Data
public class Asset {

	@Id
	@GeneratedValue
	@NotNull(groups = { AssetUpdate.class, AssetDelete.class })
	@Min(value = 1, groups = { AssetUpdate.class, AssetDelete.class })
	private long id;

	@NotEmpty(groups = { AssetCreate.class, AssetUpdate.class })
	private String name;

	private long amount;

	// @JsonIgnore
	@OneToOne
	@Valid
	private Bookkeeping bookkeeping;

	@NotNull(groups = { AssetCreate.class, AssetUpdate.class })
	@Enumerated(EnumType.STRING)
	private AssetType assetType;

	public interface AssetCreate {
	}

	public interface AssetUpdate {
	}

	public interface AssetDelete {
	}
}

package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 자산
 * @author bluesky
 *
 */
@Entity
@Data
public class Asset {

	@Id
	@GeneratedValue
	@NotNull(groups = AssetUpdate.class)
	private long id;
	
	@NotEmpty(groups = { AssetCreate.class, AssetUpdate.class })
	private String name;
	
	private long amount;
	
//	@JsonIgnore
	@OneToOne
	@Valid
	private Bookkeeping bookkeeping;
	
	@NotNull(groups = { AssetCreate.class, AssetUpdate.class })
	@Enumerated(EnumType.STRING)
	private AssetType assetType;
	
	public interface AssetCreate {
	};
	
	public interface AssetUpdate {
	};
}

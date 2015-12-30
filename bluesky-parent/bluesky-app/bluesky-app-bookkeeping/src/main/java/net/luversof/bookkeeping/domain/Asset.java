package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	@NotNull(groups = Modify.class)
	private long id;
	
	@NotEmpty(groups = { Add.class, Modify.class })
	private String name;
	
	private long amount;
	
	@JsonIgnore
	@OneToOne
	private Bookkeeping bookkeeping;
	
	@NotNull(groups = { Add.class, Modify.class })
	@Enumerated(EnumType.STRING)
	private AssetType assetType;
	
	public interface Add {
	};
	
	public interface Modify {
	};
}

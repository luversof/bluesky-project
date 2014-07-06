package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

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
	private Long id;
	
	@NotEmpty(groups = { Add.class, Modify.class })
	private String name;
	
	@Column(columnDefinition="int default 0")
	private Long amount;
	
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

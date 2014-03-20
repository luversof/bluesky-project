package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

/**
 * 자산 정보
 * @author bluesky
 *
 */
@Entity
@Data
public class Asset {

	@Id
	@GeneratedValue
	private long id;
	
	private String name;
	
	private long amount;
	
	@OneToOne
	private User user;
	
	@OneToOne
	private AssetGroup assetGroup;
}

package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
	private long id;
	
	private String name;
	
	private long amount;
	
	@Column(name = "user_id")
	private long userId;
	
	
	@Enumerated(EnumType.STRING)
	private AssetType assetType;
}

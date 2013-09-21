package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

/**
 * 유저별 자산그룹
 * @author bluesky
 *
 */
@Entity
@Data
public class AssetGroup {
	@Id
	@GeneratedValue
	private long id;
	
	private String name;
	
	private String username;
	
	@Enumerated(EnumType.ORDINAL)
	private AssetType assetType;
}

package net.luversof.asset.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class AssetGroup {
	@Id
	@GeneratedValue
	private long id;
	
	private String name;
	
	private String username;
	
	private long assetType_id;
}

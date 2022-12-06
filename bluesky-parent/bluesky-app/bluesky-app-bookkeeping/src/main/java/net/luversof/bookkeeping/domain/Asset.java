package net.luversof.bookkeeping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 자산
 * 
 * @author bluesky
 *
 */
@Data
@Entity
@Table(indexes = { @Index(name = "UK_asset_assetId", columnList = "assetId", unique = true), @Index(name = "IDX_asset_bookkeepingId", columnList = "bookkeeping_id") })
public class Asset {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = { Update.class, Delete.class })
	@Column(length = 36, nullable = false)
	private String assetId;

	@NotBlank(groups = { Update.class, Delete.class })
	@Column(name = "bookkeeping_id", length = 36, nullable = false)
	private String bookkeepingId;
	
	@Column(name = "assetGroup_id", length = 36, nullable = false)
	private String assetGroupId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;

//	private long amount;	// 이거 어떻게 계산하는게 좋을까? 

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

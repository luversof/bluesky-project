package net.luversof.api.bookkeeping.domain;

import java.util.UUID;

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
@Table(indexes = { @Index(name = "IDX_asset_bookkeepingId", columnList = "bookkeeping_id") })
public class Asset {

	@NotBlank(groups = { Update.class, Delete.class })
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@NotBlank(groups = { Update.class, Delete.class })
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Column(name = "assetGroup_id", nullable = false)
	private UUID assetGroupId;
	
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

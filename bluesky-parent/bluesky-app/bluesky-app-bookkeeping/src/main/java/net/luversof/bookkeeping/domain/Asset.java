package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Update.class, Delete.class })
	private long id;
	
	@Column(length = 36, nullable = false, unique = true)
	private String assetId;

	@Column(name = "bookkeeping_id", length = 36, nullable = false)
	private String bookkeepingId;
	
	@Column(name = "assetGroup_id", length = 36, nullable = false)
	private String assetGroupId;
	
	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	private long amount;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}

package net.luversof.api.bookkeeping.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import net.luversof.api.bookkeeping.constant.AssetGroupType;

/**
 * 카드와 체크카드를 제외한 나머지는 유저의 자유로운 변경이 가능해야함
 * 
 * @author luver
 *
 */
@Data
@Entity
@Table(indexes = { @Index(name = "UK_assetGroup_assetGroupId", columnList = "assetGroupId", unique = true), @Index(name = "IDX_assetGroup_bookkeepingId", columnList = "bookkeeping_id") })
public class AssetGroup {

	@NotBlank(groups = { Update.class, Delete.class })
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotBlank(groups = { Create.class, Update.class })
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Enumerated(EnumType.STRING)
	private AssetGroupType assetGroupType;

	@NotBlank(groups = { Create.class, CreateParam.class, Update.class })
	private String name;

	public static interface Create {
	}

	public static interface CreateParam {
	}

	public static interface Update {
	}

	public static interface Delete {
	}
}

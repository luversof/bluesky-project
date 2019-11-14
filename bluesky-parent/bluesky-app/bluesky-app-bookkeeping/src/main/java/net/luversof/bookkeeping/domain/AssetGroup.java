package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import org.hibernate.envers.Audited;

import lombok.Data;
import net.luversof.bookkeeping.constant.AssetGroupType;

/**
 * 카드와 체크카드를 제외한 나머지는 유저의 자유로운 변경이 가능해야함
 * 
 * @author luver
 *
 */
@Entity
@Data
@Audited
public class AssetGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Update.class, Delete.class, Asset.Create.class })
	private long id;

	@NotBlank(groups = { Create.class, Update.class })
	private String name;

	@ManyToOne
	private Bookkeeping bookkeeping;

	@Enumerated(EnumType.STRING)
	private AssetGroupType assetGroupType;
	
	public static interface Create {}
	public static interface Update {}
	public static interface Delete {}
}

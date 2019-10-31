package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

import lombok.Data;
import net.luversof.bookkeeping.constant.AssetGroupType;

/**
 * 카드와 체크카드를 제외한 나머지는 유저의 자유로운 변경이 가능해야함
 * @author luver
 *
 */
@Entity
@Data
@Audited
public class AssetGroup {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	private String name;
	
	@ManyToOne
	private Bookkeeping bookkeeping;
	
	private AssetGroupType assetGroupType;
}

package net.luversof.api.bookkeeping.domain;

import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.converter.AssetTypeCodeConverter;

/**
 * 계좌 유형 정의
 * 유저 별로 따로 정의하여 사용할 수 있음
 */
@Data
@Entity
@Table(name = "AssetType", indexes = { @Index(name = "IX_assetType", columnList = "bookkeeping_id") })
public class AssetType {

	@Id
	private UUID id;
	
	@NotNull(groups = { Create.class , Update.class })
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@NotNull(groups = { Create.class , Update.class })
	@Convert(converter = AssetTypeCodeConverter.class)
	private AssetTypeCode code;
	
	@NotBlank(groups = { Create.class , Update.class })
	private String name;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}

}

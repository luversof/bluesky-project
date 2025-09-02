package net.luversof.api.bookkeeping.domain;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;

/**
 * 계좌 유형 정의
 * 유저 별로 따로 정의하여 사용할 수 있음
 */
@Data
@Table(name = "AssetType")
public class AssetType {

	@Id
	private UUID id;
	
	@NotNull(groups = { Create.class , Update.class })
	@Column("bookkeeping_id")
	private UUID bookkeepingId;
	
	@NotNull(groups = { Create.class , Update.class })
//	@Convert(converter = AssetTypeCodeConverter.class)
	private AssetTypeCode code;
	
	@NotBlank(groups = { Create.class , Update.class })
	private String name;
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}

}

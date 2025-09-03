package net.luversof.api.bookkeeping.domain;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Table("Asset")
public class Asset {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	@Column("id")
	private UUID id;

	@Column("bookkeeping_id")
	@NotNull(groups = { Update.class, Delete.class })
	private AggregateReference<Bookkeeping, UUID> bookkeepingId;
	
	@Column("assetType_id")
	private AssetType assetType;

	private Map<String, Object> jsonConfig;
	
	private String name;
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}

}

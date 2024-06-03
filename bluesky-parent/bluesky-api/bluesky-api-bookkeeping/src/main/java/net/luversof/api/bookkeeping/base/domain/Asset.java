package net.luversof.api.bookkeeping.base.domain;

import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_asset_ledgerId", columnList = "ledger_id") })
public class Asset {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@NotNull(groups = { Update.class, Delete.class })
	@Column(name = "ledger_id", nullable = false)
	private UUID ledgerId;
	
	@Column(name = "assetType_id", nullable = false)
	private UUID assetTypeId;
	
	private String name;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}

}

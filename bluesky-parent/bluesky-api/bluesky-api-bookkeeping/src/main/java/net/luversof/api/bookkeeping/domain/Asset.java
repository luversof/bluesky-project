package net.luversof.api.bookkeeping.domain;

import java.util.List;
import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;
import net.luversof.api.bookkeeping.converter.IntegerListConverter;

@Data
@Entity
@Table(name = "Asset")
public class Asset {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	private UUID id;
	
	@NotNull(groups = { Update.class, Delete.class })
	
	@ManyToOne
	@JoinColumn(name = "bookkeeping_id", nullable = false, foreignKey = @ForeignKey(name = "FK_asset"))
	private Bookkeeping bookkeeping;
	
	@ManyToOne
	@JoinColumn(name = "assetType_id", nullable = false, foreignKey = @ForeignKey(name = "FK_asset2"))
	private AssetType assetType;

//	@Convert(converter = BitSetConverter.class)
	@Convert(converter = IntegerListConverter.class)
	private List<Integer> bitConfigIndexList;
	
	private String name;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}
	
	@Data
	public static class AssetConfig {
		
	}

}

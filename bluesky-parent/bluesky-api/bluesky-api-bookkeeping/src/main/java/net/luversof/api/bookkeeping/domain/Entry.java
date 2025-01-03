package net.luversof.api.bookkeeping.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * entry는 대상 account에 대한 credit, debit 정보 중 하나를 저장
 * 다중 기록을 하며 credit + debit의 총 합은 무조건 0 
 */
@Data
@Entity
@Table(
	name = "Entry",
	indexes = {
		@Index(name = "IX_entry", columnList = "incomeAsset_id"),
		@Index(name = "IX_entry2", columnList = "outgoingAsset_id"),
		@Index(name = "IX_entry3", columnList = "entryType_id")
	}
)
public class Entry implements Serializable {

	private static final long serialVersionUID = 1L;

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
//	@UuidGenerator(style = Style.TIME)
	private UUID id;
	
	@NotNull(groups = { Create.class, Update.class })
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@ManyToOne
	@JoinColumn(name = "entryType_id", nullable = false)
//	@Column(name = "entryType_id", nullable = false)
	private EntryType entryType;

	@NotNull(groups = { Create.class, Update.class })
	@Column(nullable = false)
	private ZonedDateTime entryDate;
	
	@NotNull(groups = { Create.class, Update.class })
	@Column(name = "incomeAsset_id", nullable = false)
	private UUID incomeAssetId;
	
	@NotNull(groups = { Create.class, Update.class })
	@Column(name = "outgoingAsset_id", nullable = false)
	private UUID outgoingAssetId;
	
	@NotNull(groups = { Create.class, Update.class })
	private BigDecimal amount;
	
	@JdbcTypeCode(SqlTypes.JSON)
	private EntryExtraData extraData;

	@PrePersist
	public void prePersist() {
		id = GUID.v7().toUUID();
	}
	
	public interface Create {}
    public interface Update {}
    public interface Delete {}
    
    @Data
    public static class EntryExtraData implements Serializable {

    	private static final long serialVersionUID = 1L;
    	
		private String memo;
    }

}

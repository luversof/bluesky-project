package net.luversof.api.bookkeeping.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * entry는 대상 account에 대한 credit, debit 정보 중 하나를 저장
 * 다중 기록을 하며 credit + debit의 총 합은 무조건 0 
 */
@Data
@Table("Entry")
public class Entry implements Serializable {

	private static final long serialVersionUID = 1L;

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
//	@UuidGenerator(style = Style.TIME)
	private UUID id;
	
	@NotNull(groups = { Create.class, Update.class })
	@Column("bookkeeping_id")
	private UUID bookkeepingId;
	
	@Column("entryType_id")
	private EntryType entryType;

	@NotNull(groups = { Create.class, Update.class })
	@Column("entryDate")
	private OffsetDateTime entryDate;
	
	@NotNull(groups = { Create.class, Update.class })
	@Column("incomeAsset_id")
	private UUID incomeAssetId;
	
	@NotNull(groups = { Create.class, Update.class })
	@Column("outgoingAsset_id")
	private UUID outgoingAssetId;
	
	@NotNull(groups = { Create.class, Update.class })
	private BigDecimal amount;
	
	private EntryExtraData extraData;
	
	public interface Create {}

	public interface Update {}

	public interface Delete {}

	@Data
	public static class EntryExtraData implements Serializable {

		private static final long serialVersionUID = 1L;

		private String memo;
	}

}

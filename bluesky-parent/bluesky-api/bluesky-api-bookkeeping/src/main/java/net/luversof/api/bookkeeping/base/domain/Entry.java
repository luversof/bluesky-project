package net.luversof.api.bookkeeping.base.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * entry는 대상 account에 대한 credit, debit 정보 중 하나를 저장
 * 다중 기록을 하며 credit + debit의 총 합은 무조건 0 
 */
@Data
@Entity
@Table(indexes = { @Index(name = "IDX_entry_accountId", columnList = "account_id") })
public class Entry implements Serializable {

	private static final long serialVersionUID = 1L;

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
//	@UuidGenerator(style = Style.TIME)
	private UUID id;

	@Column(name = "account_id", nullable = false)
	private UUID accountId;
	
	@Column(name = "entryTransaction_id", nullable = false)
	private UUID entryTransactionId;

	/**
	 * 차변, 들어오는 돈
	 */
	private BigDecimal debit;
	
	/**
	 * 대변 - 나가는 돈
	 */
	private BigDecimal credit;

	@PrePersist
	public void prePersist() {
		id = GUID.v7().toUUID();
	}
	
	public interface Create {}
    public interface Update {}
    public interface Delete {}

}

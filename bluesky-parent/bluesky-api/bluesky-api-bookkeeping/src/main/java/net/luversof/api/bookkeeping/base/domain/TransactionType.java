package net.luversof.api.bookkeeping.base.domain;

import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_transactionType_ledgerId", columnList = "ledger_id") })
public class TransactionType {

	@Null(groups = Create.class)
	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "ledger_id", nullable = false)
	private UUID ledgerId;
	
	@Enumerated(EnumType.STRING)
	private TransactionTypeCode code;
	
	private String name;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }
	
	public interface Create {}
	public interface Update {} 
	public interface Delete {}

}
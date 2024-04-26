package net.luversof.api.bookkeeping.base.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import net.luversof.api.bookkeeping.base.domain.Entry.Create;
import net.luversof.api.bookkeeping.base.domain.Entry.Update;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_entryTransaction_entryTransactionTypeId", columnList = "entryTransactionType_id") })
public class EntryTransaction {
	
	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
//	@UuidGenerator(style = Style.TIME)
	private UUID id;
	
	@Column(name = "entryTransactionType_id", nullable = false)
	private UUID entryTransactionTypeId;
	
	@Column(nullable = false)
	private ZonedDateTime transactionDate;

	private String memo;

	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }

}

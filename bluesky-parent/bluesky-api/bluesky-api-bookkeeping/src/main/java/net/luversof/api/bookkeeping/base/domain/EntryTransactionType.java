package net.luversof.api.bookkeeping.base.domain;

import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_entryTransactionType_bookkeepingId", columnList = "bookkeeping_id") })
public class EntryTransactionType {

	@Null(groups = Create.class)
	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Enumerated(EnumType.STRING)
	private EntryTransactionTypeCode code;
	
	private String name;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }
	
	public interface Create {}
	public interface Update {} 
	public interface Delete {}

}
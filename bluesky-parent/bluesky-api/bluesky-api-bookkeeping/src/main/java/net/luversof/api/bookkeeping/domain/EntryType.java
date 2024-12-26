package net.luversof.api.bookkeeping.domain;

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
@Table(name = "EntryType", indexes = { @Index(name = "IX_entryType", columnList = "bookkeeping_id") })
public class EntryType {

	@Null(groups = Create.class)
	@Id
	private UUID id;
	
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Enumerated(EnumType.STRING)
	private EntryTypeCode code;
	
	private String name;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }
	
	public interface Create {}
	public interface Update {} 
	public interface Delete {}

}
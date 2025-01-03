package net.luversof.api.bookkeeping.domain;

import java.io.Serializable;
import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Null;
import lombok.Data;
import net.luversof.api.bookkeeping.constant.EntryTypeCode;
import net.luversof.api.bookkeeping.converter.EntryTypeCodeConverter;

@Data
@Entity
@Table(name = "EntryType", indexes = { @Index(name = "IX_entryType", columnList = "bookkeeping_id") })
public class EntryType implements Serializable {

	private static final long serialVersionUID = 1L;

	@Null(groups = Create.class)
	@Id
	private UUID id;
	
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Convert(converter = EntryTypeCodeConverter.class)
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
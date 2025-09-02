package net.luversof.api.bookkeeping.domain;

import java.io.Serializable;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.Null;
import lombok.Data;
import net.luversof.api.bookkeeping.constant.EntryTypeCode;

@Data
@Table("EntryType")
public class EntryType implements Serializable {

	private static final long serialVersionUID = 1L;

	@Null(groups = Create.class)
	@Id
	private UUID id;
	
	@Column("bookkeeping_id")
	private UUID bookkeepingId;
	
	private EntryTypeCode code;
	
	private String name;
	
	public interface Create {}
	public interface Update {} 
	public interface Delete {}

}
package net.luversof.api.bookkeeping.base.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_entryType_bookkeepingId", columnList = "bookkeeping_id") })
public class EntryType {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "bookkeeping_id")
	private UUID bookkeepingId;
	
	@Enumerated(EnumType.STRING)
	private EntryTypeCode code;
	
	private String name;

}
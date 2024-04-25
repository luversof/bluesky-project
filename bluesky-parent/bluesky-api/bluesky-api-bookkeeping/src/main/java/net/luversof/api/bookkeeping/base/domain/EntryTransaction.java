package net.luversof.api.bookkeeping.base.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_entryTransaction_bookkeepingId", columnList = "bookkeeping_id") })
public class EntryTransaction {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Column(updatable = false)
	@CreationTimestamp
	private ZonedDateTime createdDate;

}

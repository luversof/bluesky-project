package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

/**
 * 이체인 경우 이체에 대한 기록을 남김
 * @author bluesky
 *
 */
@Entity
@Data
public class EntryTransferLog {
	@Id
	@GeneratedValue
	private long id;
	
	@OneToOne
	private Entry entryDebit;
	
	@OneToOne
	private Entry entryCredit;
	
	private long amount;
}

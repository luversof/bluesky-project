package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

import org.joda.time.DateTime;

@Entity
@Data
public class Entry {

	@Id
	@GeneratedValue
	private long id;
	
	@OneToOne
	private Asset asset;
	
	@OneToOne
	private EntryGroup entryGroup;
	
	private long amount;
	
	private DateTime date;
	
	private String memo;
	
	/*
	 * 이체인 경우 표시하기 위한 속성
	 * 이체 대상을 저장해야 하는 이슈가 있음
	 */
	private boolean transferEntry;
}

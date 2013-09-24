package net.luversof.bookkeeping.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

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
	
	private Date date;
	
	private String memo;
	
	/*
	 * 이체인 경우 표시하기 위한 속성
	 */
	private boolean isDoubleEntry;
}

package net.luversof.bookkeeping.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
@Data
public class Entry implements Serializable {
	private static final long serialVersionUID = -5106564257765676653L;

	@Id
	@GeneratedValue
	private long id;
	
	@OneToOne
	private Asset asset;
	
	@OneToOne
	private EntryGroup entryGroup;
	
	private long amount;
	
	
	@Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime date;
	
	private String memo;
	
	/*
	 * 이체인 경우 표시하기 위한 속성
	 * 이체 대상을 저장해야 하는 이슈가 있음
	 */
	private boolean transferEntry;
}

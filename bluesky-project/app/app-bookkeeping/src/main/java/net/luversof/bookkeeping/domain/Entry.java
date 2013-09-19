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
	
	private EntryType entryType;
	
	private long amount;
	
	private Date date;
	
	private String log;
}

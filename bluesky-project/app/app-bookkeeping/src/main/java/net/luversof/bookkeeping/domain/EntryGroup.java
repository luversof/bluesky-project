package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

//@Entity
//@Data
public class EntryGroup {
	@Id
	@GeneratedValue
	private long id;
	
	private String name;
	
	private String username;
	
	private long entryType_id;
}

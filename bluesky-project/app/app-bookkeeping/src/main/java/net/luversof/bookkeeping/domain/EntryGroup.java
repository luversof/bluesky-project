package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class EntryGroup {
	@Id
	@GeneratedValue
	private long id;
	
	private String name;
	
	private String username;
	
	@Enumerated(EnumType.ORDINAL)
	private EntryType entryType;
}

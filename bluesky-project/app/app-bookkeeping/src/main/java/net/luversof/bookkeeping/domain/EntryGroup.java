package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

@Entity
@Data
public class EntryGroup {
	@Id
	@GeneratedValue
	private long id;
	
	private String name;
	
	@Enumerated(EnumType.STRING)
	private EntryType entryType;
	
	@OneToOne
	private User user;
}

package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
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
	
	@Enumerated(EnumType.STRING)
	private EntryType entryType;
	
	@Column(name = "user_id")
	private long userId;
}

package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class Bookkeeping {
	
	@Id
	@GeneratedValue
	private long id;
	
	@Column(name = "user_id")
	private long userId;
}

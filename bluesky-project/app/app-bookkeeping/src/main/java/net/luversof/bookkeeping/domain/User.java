package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Data
public class User {
	@Id
	long id;
	
	@Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	@CreatedDate
	@Column(updatable = false)
	private DateTime createdDate;
}

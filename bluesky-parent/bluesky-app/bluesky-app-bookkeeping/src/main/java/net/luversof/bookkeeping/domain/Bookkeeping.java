package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

@Entity
@Data
public class Bookkeeping {

	@Id
	@GeneratedValue
	@NotEmpty(groups = Modify.class)
	private long id;

	@NotEmpty(groups = { Add.class, Modify.class })
	private String name;

	@Column(name = "user_id")
	private long userId;

	public interface Add {
	};
	
	public interface Modify {
	};
}

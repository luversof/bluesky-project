package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

@Entity
@Data
public class Bookkeeping {

	@Id
	@GeneratedValue
	@NotNull(groups = Modify.class)
	private Long id;

	@NotEmpty(groups = { Add.class, Modify.class })
	private String name;

	@Column(name = "user_id")
	private Long userId;

	public interface Add {
	};

	public interface Modify {
	};
}

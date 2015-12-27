package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_Bookkeeping_userId", columnList = "user_id") )
public class Bookkeeping {

	@Id
	@GeneratedValue
	@NotNull(groups = Modify.class)
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

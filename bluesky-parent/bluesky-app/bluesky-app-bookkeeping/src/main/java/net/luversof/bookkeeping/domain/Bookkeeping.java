package net.luversof.bookkeeping.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_Bookkeeping_userId", columnList = "user_id"))
public class Bookkeeping {

	@Id
	@GeneratedValue
	@Min(value = 1, groups = {Update.class, Delete.class})
	private long id;

	@NotEmpty(groups = { Create.class, Update.class })
	private String name;

	@Column(name = "user_id", updatable = false)
	@Min(value = 1, groups = Update.class)
	private long userId;

	public interface Create {};

	public interface Update {};
	
	public interface Delete {}
}

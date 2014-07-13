package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
public class EntryGroup {
	@Id
	@GeneratedValue
	@NotNull(groups = Modify.class)
	private long id;

	@NotEmpty(groups = { Add.class, Modify.class })
	private String name;

	@NotNull(groups = { Add.class, Modify.class })
	@Enumerated(EnumType.STRING)
	private EntryType entryType;

	@JsonIgnore
	@OneToOne
	private Bookkeeping bookkeeping;
	
	public interface Add {
	};
	
	public interface Modify {
	};
}

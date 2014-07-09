package net.luversof.bookkeeping.domain;

import java.io.Serializable;
import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.Data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

@Entity
@Data
public class Entry implements Serializable {
	private static final long serialVersionUID = -5106564257765676653L;

	@Id
	@GeneratedValue
	@NotNull(groups = Modify.class)
	private Long id;
	
	@OneToOne
	private Bookkeeping bookkeeping;
	
	@OneToOne
	@JoinColumn(name = "debit_asset_id")
	private Asset debitAsset;
	
	@OneToOne
	@JoinColumn(name = "credit_asset_id")
	private Asset creditAsset;
	
	@OneToOne
	private EntryGroup entryGroup;
	
	@NotNull(groups = { Add.class, Modify.class })
	private Long amount;
	
	@JsonDeserialize(using = LocalDateDeserializer.class)
	@NotNull(groups = { Add.class, Modify.class })
	private LocalDate entryDate;
	
	private String memo;
	
	public interface Add {
	};
	
	public interface Modify {
	};
}

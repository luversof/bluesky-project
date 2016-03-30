package net.luversof.bookkeeping.domain;

import java.io.Serializable;
import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import lombok.Data;

@Entity
@Data
public class Entry implements Serializable {
	private static final long serialVersionUID = -5106564257765676653L;

	@Id
	@GeneratedValue
	@NotNull(groups = {EntryUpdate.class, EntryDelete.class})
	@Min(value = 1, groups = {EntryUpdate.class, EntryDelete.class})
	private long id;
	
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
	
	@NotNull(groups = { EntryCreate.class, EntryUpdate.class })
	private long amount;
	
	@NotNull(groups = { EntryCreate.class, EntryUpdate.class })
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate entryDate;
	
	private String memo;
	
	public interface EntryCreate {
	};
	
	public interface EntryUpdate {
	};
	
	public interface EntryDelete {
	}
}

package net.luversof.bookkeeping.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.Data;
import net.luversof.core.LocalDateTimeDeserializer;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
	
	@DateTimeFormat(pattern="yyyy-MM-dd",iso = ISO.DATE)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@NotNull(groups = { Add.class, Modify.class })
	private LocalDateTime entryDate;
	
	private String memo;
	
	public interface Add {
	};
	
	public interface Modify {
	};
}

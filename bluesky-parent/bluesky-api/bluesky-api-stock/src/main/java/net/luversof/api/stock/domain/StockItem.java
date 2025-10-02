package net.luversof.api.stock.domain;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * 주식 종목
 */
@Data
@Table("StockItem")
public class StockItem {

	@Id
	@Column("id")
	private UUID id;

	private String ticker;
	
	private String name;
	
	private String market;

}

package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import net.luversof.api.stock.constant.TradeType;

/**
 * 주식 매매 내역
 */
@Data
@Table("Trade")	
public class Trade {

	@Id
	@Column("id")
	private UUID id;
	
	@Column("stockItem_id")
	private UUID stockItemid;
	
	private TradeType type;
	
	private int quantity;
	
	private BigDecimal price;
	
	private BigDecimal fee;
	
	private BigDecimal tax;
	
	@Column("tradeDate")
	private OffsetDateTime tradeDate;

}
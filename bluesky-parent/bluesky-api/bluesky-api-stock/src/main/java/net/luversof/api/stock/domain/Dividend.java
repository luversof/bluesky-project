package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * 주식 배당 내역
 */
@Data
@Table("Dividend")
public class Dividend {
	
	@Id
	@Column("id")
	private UUID id;
	
	@Column("account_id")
	private UUID accountId;
	
	@Column("stockItem_id")
	private UUID stockItemid;
	
	private BigDecimal price;
	
	private BigDecimal tax;
	
	@Column("payDate")
	private OffsetDateTime recordDate;	// 배당기준일
	
	@Column("payDate")
	private OffsetDateTime payDate;		// 배당지급일

}

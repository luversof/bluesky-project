package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
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

	@Column("type")
	private String type;

	@Column("quantity")
	private Integer quantity;

	@Column("price")
	private BigDecimal price;

	@Column("fee")
	private BigDecimal fee;

	@Column("tax")
	private BigDecimal tax;

	@Column("recordDate")
	private Instant recordDate; // 배당기준일

	@Column("payDate")
	private Instant payDate; // 배당지급일

}

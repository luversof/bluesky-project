package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;

@Table("StockPriceHistory")
public class StockPriceHistory {

	@Id
	@Column("id")
	private UUID id;

	@NotNull
	@Column("stockItem_id")
	private UUID stockItemId;

	@NotNull
	@Column("price")
	private BigDecimal price;

	/**
	 * 가격이 적용된 날짜/시각 (보통 일별 종가 기준으로 사용)
	 */
	@NotNull
	@Column("priceDate")
	private Instant priceDate;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getStockItemId() {
		return stockItemId;
	}

	public void setStockItemId(UUID stockItemId) {
		this.stockItemId = stockItemId;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Instant getPriceDate() {
		return priceDate;
	}

	public void setPriceDate(Instant priceDate) {
		this.priceDate = priceDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StockPriceHistory other = (StockPriceHistory) obj;
		return Objects.equals(id, other.id) && Objects.equals(price, other.price)
				&& Objects.equals(priceDate, other.priceDate) && Objects.equals(stockItemId, other.stockItemId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, price, priceDate, stockItemId);
	}

	@Override
	public String toString() {
		return "StockPriceHistory [id=" + id + ", stockItemId=" + stockItemId + ", price=" + price + ", priceDate="
				+ priceDate + "]";
	}
}

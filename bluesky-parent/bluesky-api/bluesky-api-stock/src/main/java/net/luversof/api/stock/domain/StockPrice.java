package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * 주식 현재가 정보
 */
@Table("StockPrice")
public class StockPrice {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	@Column("id")
	private UUID id;

	@NotNull(groups = { Create.class, Update.class })
	@Column("stockItem_id")
	private UUID stockItemId;

	@NotNull(groups = { Create.class, Update.class })
	@Column("price")
	private BigDecimal price;

	@LastModifiedDate
	@Column("updatedDate")
	private Instant updatedDate;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}

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

	public Instant getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Instant updatedDate) {
		this.updatedDate = updatedDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StockPrice other = (StockPrice) obj;
		return Objects.equals(id, other.id) && Objects.equals(price, other.price)
				&& Objects.equals(stockItemId, other.stockItemId) && Objects.equals(updatedDate, other.updatedDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, price, stockItemId, updatedDate);
	}

	@Override
	public String toString() {
		return "StockPrice [id=" + id + ", stockItemId=" + stockItemId + ", price=" + price + ", updatedDate="
				+ updatedDate + "]";
	}
}

package net.luversof.api.stock.domain;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("StockItemTag")
public class StockItemTag {

	@Id
	@Column("id")
	private UUID id;

	@Column("stockItem_id")
	private UUID stockItemId;

	@Column("tag")
	private String tag;

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

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		StockItemTag other = (StockItemTag) obj;
		return Objects.equals(id, other.id)
				&& Objects.equals(stockItemId, other.stockItemId)
				&& Objects.equals(tag, other.tag);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, stockItemId, tag);
	}

	@Override
	public String toString() {
		return "StockItemTag [id=" + id + ", stockItemId=" + stockItemId + ", tag=" + tag + "]";
	}
}
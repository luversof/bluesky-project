package net.luversof.api.stock.domain;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 주식 종목
 */
@Table("StockItem")
public class StockItem {

	@Id
	@Column("id")
	private UUID id;

	@Column("ticker")
	private String ticker;

	@Column("name")
	private String name;

	@Column("market")
	private String market;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StockItem other = (StockItem) obj;
		return Objects.equals(id, other.id) && Objects.equals(market, other.market) && Objects.equals(name, other.name)
				&& Objects.equals(ticker, other.ticker);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, market, name, ticker);
	}

	@Override
	public String toString() {
		return "StockItem [id=" + id + ", ticker=" + ticker + ", name=" + name + ", market=" + market + "]";
	}
}

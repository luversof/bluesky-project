package net.luversof.api.stock.domain;

import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 주식 종목 */
@Table("StockItem")
public class StockItem {

    @Id
    @Column("id")
    private UUID id;

    /** KRX, NASDAQ, NYSE 등 */
    @Column("market")
    private String market;

    /** KRX : 005930, NASDAQ : AAPL 등 */
    @Column("symbol")
    private String symbol;

    @Column("name")
    private String name;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        StockItem other = (StockItem) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(market, other.market)
                && Objects.equals(name, other.name)
                && Objects.equals(symbol, other.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, market, name, symbol);
    }

    @Override
    public String toString() {
        return "StockItem [id="
                + id
                + ", symbol="
                + symbol
                + ", name="
                + name
                + ", market="
                + market
                + "]";
    }
}

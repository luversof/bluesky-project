package net.luversof.app.google.stock.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.luversof.app.google.stock.databind.StockCurrencyDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSheetStockItem {

    @JsonProperty("종목코드")
    private String 종목코드;

    @JsonProperty("종목이름")
    private String 종목이름;

    @JsonProperty("현재가")
    @JsonDeserialize(using = StockCurrencyDeserializer.class)
    private BigDecimal 현재가;

    public String get종목코드() {
        return 종목코드;
    }

    public void set종목코드(String 종목코드) {
        this.종목코드 = 종목코드;
    }

    public String get종목이름() {
        return 종목이름;
    }

    public void set종목이름(String 종목이름) {
        this.종목이름 = 종목이름;
    }

    public BigDecimal get현재가() {
        return 현재가;
    }

    public void set현재가(BigDecimal 현재가) {
        this.현재가 = 현재가;
    }

    @Override
    public String toString() {
        return "GoogleSheetStockItem [종목코드=" + 종목코드 + ", 종목이름=" + 종목이름 + ", 현재가=" + 현재가 + "]";
    }
}

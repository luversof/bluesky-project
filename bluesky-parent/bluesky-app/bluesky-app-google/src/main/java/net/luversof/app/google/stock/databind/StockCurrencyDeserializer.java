package net.luversof.app.google.stock.databind;

import java.math.BigDecimal;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StockCurrencyDeserializer extends ValueDeserializer<BigDecimal> {

  @Override
  public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
    String value = p.getString();
    if (value == null || value.isBlank()) return null;
    value = value.replace("₩", "").replace(",", "").trim();
    if (value.isEmpty()) return null;
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      System.err.println("StockCurrencyDeserializer Failed to parse: [" + value + "]");
      return null;
    }
  }
}

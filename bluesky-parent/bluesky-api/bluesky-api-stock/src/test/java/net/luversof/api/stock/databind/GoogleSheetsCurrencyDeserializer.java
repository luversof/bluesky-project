package net.luversof.api.stock.databind;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class GoogleSheetsCurrencyDeserializer extends JsonDeserializer<BigDecimal> {

	@Override
	public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String value = p.getText();
		if (value == null || value.isBlank())
			return null;
		value = value.replace("₩", "").replace(",", "").trim();
		if (value.isEmpty())
			return null;
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
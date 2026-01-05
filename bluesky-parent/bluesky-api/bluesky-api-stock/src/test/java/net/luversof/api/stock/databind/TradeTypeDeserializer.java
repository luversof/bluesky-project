package net.luversof.api.stock.databind;

import net.luversof.api.stock.constant.TradeType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class TradeTypeDeserializer extends ValueDeserializer<TradeType> {
	
	@Override
	public TradeType deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
		String value = p.getString();
		switch (value) {
		case "매수":
			return TradeType.BUY;
		case "매도":
			return TradeType.SELL;
		default:
			throw new IllegalArgumentException("Unknown TradeType: " + value);
		}
	}

}

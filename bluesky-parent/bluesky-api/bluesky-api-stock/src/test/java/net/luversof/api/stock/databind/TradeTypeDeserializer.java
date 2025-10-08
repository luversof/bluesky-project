package net.luversof.api.stock.databind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import net.luversof.api.stock.constant.TradeType;

public class TradeTypeDeserializer extends JsonDeserializer<TradeType> {
	
	@Override
	public TradeType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String value = p.getText();
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

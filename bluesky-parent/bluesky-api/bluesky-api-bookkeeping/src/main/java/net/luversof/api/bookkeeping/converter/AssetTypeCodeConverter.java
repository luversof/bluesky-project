package net.luversof.api.bookkeeping.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;

@Converter
public class AssetTypeCodeConverter implements AttributeConverter<AssetTypeCode, Integer> {

	@Override
	public Integer convertToDatabaseColumn(AssetTypeCode attribute) {
		return attribute.getCode();
	}

	@Override
	public AssetTypeCode convertToEntityAttribute(Integer dbData) {
		return AssetTypeCode.findByCode(dbData);
	}

}

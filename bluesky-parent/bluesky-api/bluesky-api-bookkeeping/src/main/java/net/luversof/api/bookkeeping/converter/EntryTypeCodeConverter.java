//package net.luversof.api.bookkeeping.converter;
//
//import jakarta.persistence.AttributeConverter;
//import jakarta.persistence.Converter;
//import net.luversof.api.bookkeeping.constant.EntryTypeCode;
//
//@Converter
//public class EntryTypeCodeConverter implements AttributeConverter<EntryTypeCode, Integer> {
//
//	@Override
//	public Integer convertToDatabaseColumn(EntryTypeCode attribute) {
//		return attribute.getCode();
//	}
//
//	@Override
//	public EntryTypeCode convertToEntityAttribute(Integer dbData) {
//		return EntryTypeCode.findByCode(dbData);
//	}
//
//}

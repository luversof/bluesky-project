package net.luversof.api.bookkeeping.converter;

import java.util.BitSet;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BitSetConverter implements AttributeConverter<BitSet, String> {
	
    @Override
    public String convertToDatabaseColumn(BitSet attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public BitSet convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // 문자열을 BitSet으로 복원 (직접 구현 필요)
        return fromStringToBitSet(dbData);
    }
    
    
    /**
     * 문자열에서 BitSet을 복원하는 메서드
     * @param bitSetString BitSet.toString()의 결과
     * @return 복원된 BitSet
     */
    public BitSet fromStringToBitSet(String bitSetString) {
        BitSet bitSet = new BitSet();

        // 문자열이 비어 있지 않을 경우 파싱
        if (bitSetString != null && bitSetString.startsWith("{") && bitSetString.endsWith("}")) {
            // 중괄호 제거 및 공백 제거
            String content = bitSetString.substring(1, bitSetString.length() - 1).trim();

            if (!content.isEmpty()) {
                // 쉼표로 나눠 인덱스 추가
                String[] indices = content.split(",");
                for (String index : indices) {
                    bitSet.set(Integer.parseInt(index.trim()));
                }
            }
        }
        return bitSet;
    }
}
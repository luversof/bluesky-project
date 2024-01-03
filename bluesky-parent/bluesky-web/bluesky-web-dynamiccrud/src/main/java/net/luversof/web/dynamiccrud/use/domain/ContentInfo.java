package net.luversof.web.dynamiccrud.use.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.FieldType;

/**
 * 응답할 데이터 목록을 가공한 정보를 담고 있는 객체
 */
public class ContentInfo {
	
	/**
	 * 키 정보를 담고 있는 맵
	 */
	@Getter
	private List<ContentKey> contentKeyList;
	
	/**
	 * 가공된 목록을 담고있는 맵 리스트
	 */
	@Getter
	private List<Map<String, Object>> processedContentMapList;
	
	public ContentInfo(List<Map<String, Object>> contentMapList, List<Field> fieldList) {
		
		// 데이터가 없으면 계산을 할 수 없음.
		if (contentMapList == null || contentMapList.isEmpty()) {
			return;
		}
		
		// contentKeyMap을 만들어보자.
		// keyMap의 순서 처리는 어떻게 해야 할까?
		// fieldList가 전체 key를 다 가지고 있지 않음.
		// fieldList의 순서와 data의 순서를 어떻게 조합하는게 좋을까?
		// 단순히 content에 있는 값을 기준으로 순서를 정한 후 fieldList의 지정된 순서로 변경이 되나?
		
		contentKeyList = new ArrayList<>();
		
		var firstContent = contentMapList.get(0);
		
		firstContent.keySet().forEach(key -> {
			if (fieldList == null || fieldList.isEmpty()) {
				contentKeyList.add(new ContentKey(key, key, FieldType.STRING));
				return;
			}
			
			var targetField = fieldList.stream().filter(x -> x.getColumn().equals(key)).findAny().orElseGet(() -> null);
			if (targetField == null) {
				contentKeyList.add(new ContentKey(key, key, FieldType.STRING));
				return;
			}
			
			if (targetField.isVisible()) {
				contentKeyList.add(new ContentKey(targetField.getColumn(), targetField.getName(), targetField.getType()));
			}
		});
		
		// SPEL 타입의 key도 추가해야 함
		fieldList.stream().filter(x -> x.getType().equals(FieldType.SPEL)).forEach(field -> {
			contentKeyList.add(new ContentKey(field.getColumn(), field.getName(), field.getType()));
		});
		
		// TODO Collections.swap(listCountries, 1, 5); 과 같이 사용하여 contentKeyList 순서를 정렬해야 함
		
		
		// 계산된 contentKeyList를 기준으로 processedContentMapList를 만든다.
		processedContentMapList = new ArrayList<>();
		for (var contentMap : contentMapList) {
			for (var contentKey : contentKeyList) {
				
				
				
				// type에 따라 적절하게 값을 처리한다.
				
			}
		}
	}

	
	/**
	 * key 값을 가공한 정보를 담은 객체 
	 * @param <K> : 원래 key 값
	 * @param <V> : 보여줄 key 값
	 */
	@Data
	@AllArgsConstructor
	public class ContentKey {
		
		private String originKey;
		private String key;
		private FieldType type;
		
	}
}

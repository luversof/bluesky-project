package net.luversof.web.dynamiccrud.use.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;

/**
 * 응답할 데이터 목록을 가공한 정보를 담고 있는 객체
 * 적당한 이름이 생각나지 않네...
 */
public class ContentInfo {
	
	/**
	 * 키 정보를 담고 있는 맵
	 */
	@Getter
	private List<ContentKey> contentKeyList;
	
	/**
	 * view 페이지 input form 처리를 위해 원본 데이터를 같이 전달해야 함.
	 * 이 때 db data에는 column type SPEL인 값이 존재하지 않으므로 해당 값도 같이 추가해서 전달
	 * 근데 이게 의미가 있나? db에 저장하는 data가 아니고 field 정의에 저장되는 값인데...
	 * SPEL의 경우 update가 불가하도록 처리가 필요할 듯 함 
	 */
	@Getter
	private List<Map<String, Object>> contentMapList;
	
	/**
	 * 가공된 목록을 담고있는 맵 리스트
	 */
	@Getter
	private List<Map<String, Object>> processedContentMapList;
	
	public ContentInfo(List<Map<String, Object>> contentMapList, List<DbField> dbFieldList) {
		
		// 데이터가 없으면 계산을 할 수 없음.
		if (contentMapList == null || contentMapList.isEmpty()) {
			return;
		}
		
		this.contentMapList = contentMapList;
		
		// contentKeyMap을 만들어보자.
		// keyMap의 순서 처리는 어떻게 해야 할까?
		// fieldList가 전체 key를 다 가지고 있지 않음.
		// fieldList의 순서와 data의 순서를 어떻게 조합하는게 좋을까?
		// 단순히 content에 있는 값을 기준으로 순서를 정한 후 fieldList의 지정된 순서로 변경이 되나?
		
		contentKeyList = new ArrayList<>();
		
		var firstContent = contentMapList.get(0);
		
		firstContent.keySet().forEach(key -> {
			if (dbFieldList == null || dbFieldList.isEmpty()) {
				contentKeyList.add(new ContentKey(key, key, Short.MAX_VALUE, DbFieldColumnType.STRING, true));
				return;
			}
			
			var targetField = dbFieldList.stream().filter(x -> x.getColumnId().equals(key)).findAny().orElseGet(() -> null);
			if (targetField == null) {
				contentKeyList.add(new ContentKey(key, key, Short.MAX_VALUE, DbFieldColumnType.STRING, true));
				return;
			}
			
			contentKeyList.add(new ContentKey(targetField.getColumnId(), targetField.getColumnName(), targetField.getFormOrder(), targetField.getColumnType(), targetField.isColumnVisible()));
		});
		
		// SPEL 타입은 DB에서 획득한 컬럼이 아니기 때문에 별도로 추가해야 함
		dbFieldList.stream().filter(x -> x.getColumnType().equals(DbFieldColumnType.SPEL)).forEach(dbField -> {
			contentKeyList.add(new ContentKey(dbField.getColumnId(), dbField.getColumnName(), dbField.getFormOrder(), dbField.getColumnType(), dbField.isColumnVisible()));
		});
		
		contentKeyList.sort(Comparator.comparing(ContentKey::getDisplayOrder));
		
		// 계산된 contentKeyList를 기준으로 processedContentMapList를 만든다.
		var expressionParser = new SpelExpressionParser();
		
		processedContentMapList = new ArrayList<>();
		for (var contentMap : this.contentMapList) {
			var evaluationContext = new StandardEvaluationContext();
			evaluationContext.setVariables(contentMap);
			
			var processedContentMap = new LinkedHashMap<String, Object>();
			for (var contentKey : contentKeyList) {
				// type에 따라 적절하게 값을 처리한다.
				// SPEL의 경우 format의 설정을 기준으로 처리
				// contentMap에도 SPEL의 value를 추가
				if (contentKey.getType().equals(DbFieldColumnType.SPEL)) {	
					var field = dbFieldList.stream().filter(x -> x.getColumnId().equals(contentKey.getOriginKey())).findAny().orElseGet(() -> null);
					if (field.getColumnFormat() == null || field.getColumnFormat().isEmpty()) {
						processedContentMap.put(contentKey.getKey(), null);	
						contentMap.put(contentKey.getOriginKey(), null);
					} else {
						var expression = expressionParser.parseExpression(field.getColumnFormat());
						
						var value = expression.getValue(evaluationContext);
						contentMap.put(contentKey.getOriginKey(), value);
						processedContentMap.put(contentKey.getKey(), value);
						
					}
				} else {
					processedContentMap.put(contentKey.getKey(), contentMap.get(contentKey.getOriginKey()));
				}
			}
			processedContentMapList.add(processedContentMap);
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
		private Short displayOrder;
		private DbFieldColumnType type;
		private boolean visible;
		
	}
}

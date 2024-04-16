package net.luversof.web.dynamiccrud.use.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import io.github.luversof.boot.context.expression.util.SpelParserUtil;
import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldVisible;

/**
 * 응답할 데이터 목록을 가공한 정보를 담고 있는 객체
 * 적당한 이름이 생각나지 않네...
 */
public class ContentInfo {
	
	/**
	 * 키 정보를 담고 있는 맵
	 */
	@Getter
	private List<ContentKeyInfo> contentKeyInfoList;
	
	/**
	 * view 페이지 input form 처리를 위해 원본 데이터를 같이 전달해야 함.
	 * 이 때 db data에는 column type SPEL인 값이 존재하지 않으므로 해당 값도 같이 추가해서 전달
	 * 근데 이게 의미가 있나? db에 저장하는 data가 아니고 field 정의에 저장되는 값인데...
	 * SPEL의 경우 update가 불가하도록 처리가 필요할 듯 함 
	 */
	@Getter
	private List<Map<String, Object>> originContentMapList;
	
	/**
	 * 가공된 목록을 담고있는 맵 리스트
	 */
	@Getter
	private List<Map<String, Object>> processedContentMapList;
	
	/**
	 * @param contentMapList 대상 데이터 목록
	 * @param dbFieldList 대상 속성 정의 목록
	 */
	public ContentInfo(List<Map<String, Object>> contentMapList, List<DbField> dbFieldList) {
		
		// 데이터가 없으면 계산을 할 수 없음.
		if (contentMapList == null || contentMapList.isEmpty()) {
			return;
		}
		
		this.originContentMapList = contentMapList;
		
		createContentKeyInfoList(dbFieldList);
		
		createProcessedContentMapList();
	}
	
	
	private void createContentKeyInfoList(List<DbField> dbFieldList) {
		contentKeyInfoList = new ArrayList<>();
		
		var firstContent = originContentMapList.get(0);
		
		// 조회한 데이터 목록의 첫번째 데이터의 key 목록을 기준으로 contentKeyList를 생성
		firstContent.keySet().forEach(key -> {
			
			// 저장된 dbField 목록이 없으면 데이터의 key를 그대로 사용
			if (dbFieldList == null || dbFieldList.isEmpty()) {
				contentKeyInfoList.add(new ContentKeyInfo(key, key, Short.MAX_VALUE, DbFieldColumnType.STRING, true, null));
				return;
			}
			
			// 저장된 dbField 목록에 해당 key에 대한 정보가 없으면 데이터의 key를 그대로 사용
			var targetField = dbFieldList.stream().filter(x -> x.getColumnId().equals(key)).findAny().orElseGet(() -> null);
			if (targetField == null) {
				contentKeyInfoList.add(new ContentKeyInfo(key, key, Short.MAX_VALUE, DbFieldColumnType.STRING, true, null));
				return;
			}

			// 사용 불가 설정된 컬럼은 무조건 제외처리
			if (!targetField.getColumnVisible().equals(DbFieldVisible.NOT_USED)) {
				contentKeyInfoList.add(new ContentKeyInfo(targetField.getColumnId(), targetField.getColumnName(), targetField.getColumnOrder(), targetField.getColumnType(), targetField.isColumnVisible(), targetField));
			}
		});
		
		// SPEL 타입은 DB에서 획득한 컬럼이 아니기 때문에 별도로 추가해야 함
		dbFieldList.stream().filter(x -> x.getColumnType().equals(DbFieldColumnType.SPEL)).forEach(dbField -> {
			contentKeyInfoList.add(new ContentKeyInfo(dbField.getColumnId(), dbField.getColumnName(), dbField.getColumnOrder(), dbField.getColumnType(), dbField.isColumnVisible(), dbField));
		});
		
		contentKeyInfoList.sort(Comparator.comparing(ContentKeyInfo::displayOrder));
	}
	
	/**
	 * 계산된 contentKeyList를 기준으로 processedContentMapList를 만든다.
	 */
	private void createProcessedContentMapList() {
		processedContentMapList = new ArrayList<>();
		for (var originContentMap : this.originContentMapList) {
			var processedContentMap = new LinkedHashMap<String, Object>();
			for (var contentKeyInfo : contentKeyInfoList) {
				// type에 따라 적절하게 값을 처리
				// SPEL의 경우 format의 설정을 기준으로 처리
				// contentMap에도 SPEL의 value를 추가한다. (contentMap은 DB에서 조회한 값이므로 SPEL 컬럼의 값이 없음)
				var dbField = contentKeyInfo.dbField;
				if (contentKeyInfo.type().equals(DbFieldColumnType.SPEL)) {	
					Object value = null;
					if (StringUtils.hasText(dbField.getColumnFormat())) {
						value = SpelParserUtil.parse(dbField.getColumnFormat(), originContentMap);
					}
					originContentMap.put(contentKeyInfo.originKey(), value);
					processedContentMap.put(contentKeyInfo.key(), value);
				} else {
					processedContentMap.put(contentKeyInfo.key(), getProcessedValue(originContentMap.get(contentKeyInfo.originKey()), dbField));
				}
			}
			processedContentMapList.add(processedContentMap);
		}
	}
	
	/**
	 * dbField의 columnPreset에 화면 표시 값이 지정되어 있는 경우 originValue를 화면 표시 값으로 변경하여 반환
	 * @param value
	 * @param dbField
	 * @return
	 */
	private Object getProcessedValue(Object originValue, DbField dbField) {
		if (dbField == null) {
			return originValue;
		}
		
		if (!StringUtils.hasText(dbField.getColumnPreset())) {
			return originValue;
		}
		
		var presetParts = dbField.getColumnPreset().split(",");
		for (var presetPart : presetParts) {
			String presetKey;
			String presetValue;
			if (presetPart.contains("|")) {
				presetKey = presetPart.split("\\|")[0];
				presetValue = presetPart.split("\\|")[1];
			} else {
				presetKey = presetPart;
				presetValue = presetPart;
			}
			if (presetKey.equals(originValue)) {
				return presetValue;
			}
		}
		return originValue;
	}

	
	/**
	 * key 값을 가공한 정보를 담은 객체 
	 */
	public record ContentKeyInfo(
			String originKey,		// 원본 key
			String key,				// 보여주기 위한 key
			Short displayOrder,		// 순서
			DbFieldColumnType type,
			boolean visible,			// 노출 여부
			DbField dbField
			) {
	}
}

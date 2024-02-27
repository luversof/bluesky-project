package net.luversof.web.dynamiccrud.use.service.mongo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.mongodb.client.MongoClient;

import io.github.luversof.boot.autoconfigure.connectioninfo.mongo.MongoClientConnectionInfoCollectorDecorator;
import io.github.luversof.boot.autoconfigure.mongo.MongoProperties;
import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldEnable;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.setting.util.SettingStringUtil;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.use.service.UseService;

@Service
public class MongoUseService implements UseService {

	@Getter
	private MongoClientConnectionInfoCollectorDecorator mongoClientConnectionInfoCollectorDecorator;
	private MongoProperties mongoProperties;

	public MongoUseService(@Nullable MongoClientConnectionInfoCollectorDecorator mongoClientConnectionInfoCollectorDecorator, MongoProperties mongoProperties) {
		this.mongoClientConnectionInfoCollectorDecorator = mongoClientConnectionInfoCollectorDecorator;
		this.mongoProperties = mongoProperties;
	}

	@Override
	public SubMenuDbType getSupportDbType() {
		return SubMenuDbType.Mongo;
	}

	private MongoClient getMongoClient(String connection) {
		return mongoClientConnectionInfoCollectorDecorator.getConnectionInfo(connection);
	}

	private String getMongoDataBase(String connection) {
		return mongoProperties.getConnectionMap().get(connection).getDatabase();
	}

	@Override
	public Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.SELECT);
		var dbFieldList = SettingUtil.getDbFieldList(settingParameter);
		var mongoClient = getMongoClient(dbQuery.getDataSourceName());
		var database = mongoClient.getDatabase(getMongoDataBase(dbQuery.getDataSourceName()));

		// 필수 검색 조건이 있는 경우 확인
		var requiredFieldList = dbFieldList.stream().filter(x -> DbFieldEnable.REQUIRED.equals(x.getEnableSearch())).toList();
		if (requiredFieldList.stream().anyMatch(x -> !dataMap.containsKey(x.getColumnId()) || !StringUtils.hasText(dataMap.get(x.getColumnId())))) {
			return new PageImpl<>(Collections.emptyList(), pageable, 0);
		}

		// 등록된 queryString 문자열 변환 처리
		var formattedQueryString = SettingStringUtil.format(dbQuery.getQueryString(), dataMap);

		var selectCommand = Document.parse(formattedQueryString);

		// 등록된 queryString에 filter 조건이 작성되어 있는 경우 페이징과 검색 조건을 생략하고 해당 쿼리 실행처리
		if (selectCommand.containsKey("filter")) {
			var result = database.runCommand(selectCommand);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) ((Document) result.get("cursor")).get("firstBatch");
			return new PageImpl<>(list, pageable, list.size());
		}

		// 검색 조건 추가
		var targetFieldList = dbFieldList.stream().filter(x -> (DbFieldEnable.REQUIRED.equals(x.getEnableSearch()) || DbFieldEnable.ENABLED.equals(x.getEnableSearch())) && dataMap.containsKey(x.getColumnId()) && StringUtils.hasText(dataMap.get(x.getColumnId()))).toList();
		Document filter = new Document();
		if (!targetFieldList.isEmpty()) {
			for (var targetField : targetFieldList) {
				Object value = null;
				if (targetField.getColumnType().equals(DbFieldColumnType.INT)) {
					value = Integer.parseInt(dataMap.get(targetField.getColumnId()));
				} else if (targetField.getColumnType().equals(DbFieldColumnType.LONG)) {
					value = Long.parseLong(dataMap.get(targetField.getColumnId()));
				} else {
					value = dataMap.get(targetField.getColumnId());
				}
				filter.put(targetField.getColumnId(), value);
			}
			selectCommand.put("filter", filter);
		}
		
		selectCommand.put("limit", pageable.getPageSize());
		selectCommand.put("skip", pageable.getOffset());
		var result = database.runCommand(selectCommand);
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("cursor", Document.class).get("firstBatch");
		
		// 첫페이지 호출에 pageSize보다 결과 값이 적은 경우 count 호출이 불필요함
		if (pageable.getOffset() == 0 && list.size() < pageable.getPageSize()) {
			return new PageImpl<>(list, pageable, list.size());
		}
		
		// count 조회
		long countDocuments = database.getCollection(selectCommand.getString("find")).countDocuments(filter);

		return new PageImpl<Map<String, Object>>(list, pageable, countDocuments);
	}

	@Override
	public Object create(SettingParameter settingParameter, Map<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.INSERT);
		return runCommand(dbQuery, dataMap);
	}

	@Override
	public Object update(SettingParameter settingParameter, Map<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.UPDATE);
		return runCommand(dbQuery, dataMap);
	}

	@Override
	public Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap) {
		var dbQuery = SettingUtil.getDbQuery(settingParameter, DbQuerySqlCommandType.DELETE);
		
		List<Map<String, String>> dataMapList = new ArrayList<>();
		
		dataMap.forEach((key, value) -> {
			// 갯수 만큼 맵을 추가한다.
			if (dataMapList.isEmpty()) {
				for (int i = 0; i < value.size() ; i++) {
					dataMapList.add(new HashMap<String, String>());
				}
			}
			
			for (int i = 0 ; i < value.size() ; i++) {
				dataMapList.get(i).put(key, value.get(i));
			}
		});
		
		List<Object> resultList = new ArrayList<Object>();
		dataMapList.forEach(map -> {
			Object result = runCommand(dbQuery, map);
			resultList.add(result);
		});
		return resultList;	
	}
	
	private Object runCommand(DbQuery dbQuery, Map<String, String> dataMap) {
		var mongoClient = getMongoClient(dbQuery.getDataSourceName());
		var database = mongoClient.getDatabase(getMongoDataBase(dbQuery.getDataSourceName()));
		// 등록된 queryString 문자열 변환 처리
		var formattedQueryString = SettingStringUtil.format(dbQuery.getQueryString(), dataMap);
		var command = Document.parse(formattedQueryString);
		Document result = database.runCommand(command);
		return result;
	}

}

package net.luversof.web.dynamiccrud.use.service.mongo;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import com.mongodb.client.MongoClient;

import io.github.luversof.boot.autoconfigure.connectioninfo.mongo.MongoClientConnectionInfoCollectorDecorator;
import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.use.service.UseService;

@Service
public class MongoUseService implements UseService {
	
	@Getter
	private MongoClientConnectionInfoCollectorDecorator mongoClientConnectionInfoCollectorDecorator; 
	
	public MongoUseService(MongoClientConnectionInfoCollectorDecorator mongoClientConnectionInfoCollectorDecorator) {
		this.mongoClientConnectionInfoCollectorDecorator = mongoClientConnectionInfoCollectorDecorator;
	}
	

	@Override
	public SubMenuDbType getSupportDbType() {
		return SubMenuDbType.Mongo;
	}
	
	
	private MongoClient getMongoClient() {
		return mongoClientConnectionInfoCollectorDecorator.getConnectionInfo("");
	}

	@Override
	public Page<Map<String, Object>> find(SettingParameter settingParameter, Pageable pageable,
			Map<String, String> dataMap) {
		// TODO Auto-generated method stub
		
		
		
		return null;
	}

	@Override
	public Object create(SettingParameter settingParameter, Map<String, String> dataMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object update(SettingParameter settingParameter, Map<String, String> dataMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap) {
		// TODO Auto-generated method stub
		return null;
	}

}

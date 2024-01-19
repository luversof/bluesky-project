package net.luversof.web.dynamiccrud.use.service.mariadb;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.use.service.AbstractDbUseService;

@Service
public class MariadbUseService extends AbstractDbUseService {
	
	@Getter
	private final SubMenuDbType supportDbType = SubMenuDbType.MySql;
	
	@Getter
	private String countQuery = "SELECT COUNT(1) FROM ${tableName} ${whereClause}";
	
	@Getter
	private String selectPagingQuery = "SELECT * FROM ${tableName} ${whereClause} ${orderClause} ${limitClause}";
	
	@Getter
	private String limitClause = "LIMIT :limit OFFSET :offset";
	
} 
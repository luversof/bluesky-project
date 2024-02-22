package net.luversof.web.dynamiccrud.use;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryCase {
	
	private QueryCaseDbType dbType;
	
	private String queryStr;

	public static List<QueryCase> of(QueryCaseDbType k1, String v1) {
		return List.of(new QueryCase(k1, v1));
	}

	public static List<QueryCase> of(QueryCaseDbType k1, String v1, QueryCaseDbType k2, String v2) {
		return List.of(new QueryCase(k1, v1), new QueryCase(k2, v2));
	}

	public static List<QueryCase> of(QueryCaseDbType k1, String v1, QueryCaseDbType k2, String v2, QueryCaseDbType k3, String v3) {
		return List.of(new QueryCase(k1, v1), new QueryCase(k2, v2), new QueryCase(k3, v3));
	}
	
	public static List<QueryCase> of(QueryCaseDbType k1, String v1, QueryCaseDbType k2, String v2, QueryCaseDbType k3, String v3, QueryCaseDbType k4, String v4) {
		return List.of(new QueryCase(k1, v1), new QueryCase(k2, v2), new QueryCase(k3, v3), new QueryCase(k4, v4));
	}

}
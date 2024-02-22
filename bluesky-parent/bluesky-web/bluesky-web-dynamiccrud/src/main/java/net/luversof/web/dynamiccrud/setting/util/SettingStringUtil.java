package net.luversof.web.dynamiccrud.setting.util;

import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SettingStringUtil {

//	/**
//	 * query에서 tableName 추출
//	 * @param query
//	 * @return
//	 */
//	public static String getTableName(String query) {
//		Pattern p = Pattern.compile("FROM\\s+(?:\\w+\\.)*(\\w+)(\\s*$|\\s+(WITH|WHERE|[LEFT|RIGHT]+\\s+[OUTER|INNER]+\\s+JOIN|ORDER\\s+BY|GROUP\\s+BY|HAVING|LIMIT|OFFSET))",Pattern.CASE_INSENSITIVE);
//        Matcher result = p.matcher(query);
//        if (result.find()) {
//        	return result.group(1);
//        }
//        return "";
//	}
//	
//	/**
//	 * query에서 order by 절 추출
//	 * @param query
//	 * @return
//	 */
//	public static String getOrderClause(String query) {
//		Pattern p = Pattern.compile("(ORDER\\s+BY.*?)(\\s*$|\\s+([LEFT|RIGHT]+\\s+[OUTER|INNER]+\\s+JOIN|ORDER\\s+BY|GROUP\\s+BY|HAVING|LIMIT))",Pattern.CASE_INSENSITIVE);
//        Matcher result = p.matcher(query);
//        if (result.find()) {
//        	return result.group(1);
//        }
//		return "";
//	}
	
	/**
	 * String을 format 처리함
	 * 
	 * @param str
	 * @param param
	 * @return
	 */
	public static String format(String str, Map<String, String> param) {
		return StringMapper.doReplace(str, param);
	}
	
//	public static boolean isCustomQuery(String query) {
//		Pattern pattern = Pattern.compile("WHERE|HAVING", Pattern.CASE_INSENSITIVE);
//		Matcher matcher = pattern.matcher(query);
//		return matcher.find() || !StringUtils.hasText(getTableName(query)); 
//	}
}

package net.luversof.web.dynamiccrud.setting.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;

/**
 * 문자열의 placeholder를 교체하는 방법을 찾지 못해 따로 구성
 */
public class StringMapper {

	public static final String MAPPING_PREFIX = "${";
	public static final String MAPPING_SUFFIX = "}";
	public static final String EMPTY_STRING = "";
	public static final String MAPPING_PATTERN = "(\\$\\{)([a-zA-Z0-9]*)(})";

	@Data
	static class Cursor {
		private int s;
		private int e;
	}

	private static Cursor find(String source, int offset) {
		int sid = source.indexOf(MAPPING_PREFIX, offset);
		int eid = source.indexOf(MAPPING_SUFFIX, sid > 0 ? sid : offset);

		Cursor csr = new Cursor();
		csr.setS(sid);
		csr.setE(eid);
		return csr;
	}

	public static String doReplace(final String str, final Map<String, String> map) {
		int offset = 0;
		String replaced = str;

		while (true) {
			Cursor csr = find(replaced, offset);
			int s = csr.getS();
			int e = csr.getE();
			if (s < 0 || e < 0) {
				break;
			} else {
				String key = replaced.substring(s + MAPPING_PREFIX.length(), e);
				String value = (map.get(key) == null ? EMPTY_STRING : map.get(key));

				int vLen = value.length();
				int oLen = MAPPING_PREFIX.length() + key.length() + MAPPING_SUFFIX.length();
				int diff = vLen - oLen;

				replaced = replaced.replace(MAPPING_PREFIX + key + MAPPING_SUFFIX, value);
				offset = e + diff;
			}
		}
		return replaced;
	}

	public static String doReplaceWithRegEx(final String str, final Map<String, String> map) {
		Pattern pattern = Pattern.compile(MAPPING_PATTERN);
		Matcher matcher = pattern.matcher(str);
		String replaced = str;

		while (matcher.find()) {
			String prefix = matcher.group(1);
			String key = matcher.group(2);
			String suffix = matcher.group(3);
			String value = (map.get(key) == null ? EMPTY_STRING : map.get(key));
			replaced = replaced.replace(prefix + key + suffix, value);
		}
		return replaced;
	}
}

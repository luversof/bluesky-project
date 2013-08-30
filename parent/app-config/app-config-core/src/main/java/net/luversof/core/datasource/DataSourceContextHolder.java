package net.luversof.core.datasource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class DataSourceContextHolder {
	private static final ThreadLocal<DataSourceType> CONTEXT_HOLDER = new ThreadLocal<>();
	
	public static void setDataSourceType(DataSourceType dataSourceType) {
		CONTEXT_HOLDER.set(dataSourceType);
	}
	
	public static DataSourceType getDataSourceType() {
		return CONTEXT_HOLDER.get();
	}
	
	public static void clearDataSourceType() {
		CONTEXT_HOLDER.remove();
	}
}

package net.luversof.api.bookkeeping.constant;

import java.util.Map;

public final class AssetJsonConfigConstant {

    private AssetJsonConfigConstant() {}

    public static final String ENABLE_DELETE = "enableDelete";
    public static final String ENABLE_UPDATE = "enableUpdate";
    public static final String ENABLE_DISPLAY = "enableDisplay";

    public static Map<String, Object> getImmutableConfigList() {
        return Map.of(
                AssetJsonConfigConstant.ENABLE_DELETE, false,
                AssetJsonConfigConstant.ENABLE_UPDATE, false,
                AssetJsonConfigConstant.ENABLE_DISPLAY, false);
    }

    public static Map<String, Object> getCustomConfigList() {
        return Map.of(
                AssetJsonConfigConstant.ENABLE_DELETE, true,
                AssetJsonConfigConstant.ENABLE_UPDATE, true,
                AssetJsonConfigConstant.ENABLE_DISPLAY, true);
    }
}

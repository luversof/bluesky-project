package net.luversof.web.dynamiccrud.setting.domain;

/**
 * request로 요청하는 검색 관련 정보
 */
public record SettingQueryParameter(String product, String mainMenu, String subMenu) {

}

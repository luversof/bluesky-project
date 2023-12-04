package net.luversof.web.dynamiccrud.use.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record UseParameter(String product, String mainMenu, String subMenu, String sqlCommandType) {

}

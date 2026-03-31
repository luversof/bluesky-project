package net.luversof.web.common.controller;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import java.util.List;
import net.luversof.web.common.menu.domain.Menu;
import net.luversof.web.common.util.WebCommonUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@DevCheckController
@RequestMapping(value = "/menu", produces = MediaType.APPLICATION_JSON_VALUE)
public class WebCommonDevCheckController {

    @GetMapping("/menuList")
    public List<Menu> getMenuList(String key) {
        return WebCommonUtil.getMenuList(key);
    }
}

package net.luversof.web.dynamiccrud.setting.controller;

import io.github.luversof.boot.exception.BlueskyException;
import java.util.List;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import org.springframework.ui.Model;

/** 동일 기능을 path 분기하여 사용하기 위해 제공된은 abstract class */
public abstract class AbstractSettingViewController implements SettingViewControllerInterface {

    /**
     * path 체크
     *
     * @param adminProjectId
     * @param projectId
     * @param mainMenuId
     * @param subMenuId
     */
    protected void checkPathVariable(
            String adminProjectId, String projectId, String mainMenuId, String subMenuId) {}
    ;

    @Override
    public String view(
            String adminProjectId,
            String projectId,
            String mainMenuId,
            String subMenuId,
            Model model) {

        checkPathVariable(adminProjectId, projectId, mainMenuId, subMenuId);

        /** (s) 상단 메뉴 처리 * */
        var subMenuList = SettingUtil.getSubMenuList(adminProjectId, projectId, mainMenuId);
        if (subMenuList.isEmpty()) {
            throw new BlueskyException("NOT_EXIST_SUBMENU");
        }

        model.addAttribute("subMenuList", subMenuList);

        var subMenu = SettingUtil.getSubMenu(adminProjectId, projectId, mainMenuId, subMenuId);
        if (subMenu == null) {
            throw new BlueskyException("NOT_EXIST_SUBMENU");
        }

        if (!subMenu.isEnableDisplay()) {
            throw new BlueskyException("NOT_USE_SUBMENU");
        }

        // Setting 정보를 기준으로 해당 데이터를 조회

        List<DbField> dbFieldList =
                SettingUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
        model.addAttribute("dbFieldList", dbFieldList);
        model.addAttribute(
                "hasSearchField", dbFieldList.stream().anyMatch(x -> x.isEnableSearch()));

        /** (e) 상단 메뉴 처리 * */
        return "use/index";
    }
}

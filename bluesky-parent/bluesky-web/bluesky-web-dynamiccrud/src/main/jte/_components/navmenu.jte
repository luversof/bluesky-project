@import java.util.List
@import net.luversof.web.common.menu.domain.Menu
@import net.luversof.web.dynamiccrud.util.TemplateUtil

@param List<Menu> menuList = TemplateUtil.getSubMenuList() != null ? TemplateUtil.getSubMenuList() : TemplateUtil.getDefaultSubMenuList()
@param boolean isCurrentMenu = false
@param boolean isDropdown = false
@param String classAppend = ""

<ul class="menu p-0 ${classAppend}">
        @for(Menu menu : menuList)
                @if(menu.isDisplay())
                        <li class="${isDropdown ? "" : (menu.isCurrentMenu() ? "" : "hidden lg:flex")}">
                                <a class="font-bold h-14 rounded-none flex ${menu.isCurrentMenu() ? "active" : ""}"
                                        href="${menu.getUrl()}">
                                        ${menu.getName()}
                                </a>
                        </li>
                @endif
        @endfor
</ul>

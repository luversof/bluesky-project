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
                                <a class="${isDropdown ? "" : "h-14 flex items-center px-3 text-base-content/70 hover:text-base-content hover:bg-transparent"}"
                                        href="${menu.getUrl()}"
                                        aria-current="${menu.isCurrentMenu() ? "page" : null}">
                                        ${menu.getName()}
                                </a>
                        </li>
                @endif
        @endfor
</ul>

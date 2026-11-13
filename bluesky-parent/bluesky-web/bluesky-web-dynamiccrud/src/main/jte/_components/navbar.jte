@import net.luversof.web.dynamiccrud.util.TemplateUtil
@import net.luversof.client.user.util.UserUtil
@import io.github.luversof.boot.context.ApplicationContextUtil
@import org.springframework.core.env.Environment

@param String navibarTitle = "Event Admin Setting"

!{var env = ApplicationContextUtil.getApplicationContext().getBean(Environment.class);}

<header class="navbar p-0 min-h-14 fixed top-0 inset-x-0 z-20 bg-base-100 border-b border-base-300">

        <div class="navbar-start">
                @if(TemplateUtil.isAdminMenu())
                        <div class="dropdown">
                                <label tabindex="0" class="btn btn-ghost btn-square h-14" aria-label="관리 프로젝트">
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                                                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
                                        </svg>
                                </label>
                                <ul tabindex="0" class="w-52 p-2 menu menu-sm dropdown-content z-30 bg-base-100 border border-base-300">
                                        <li>
                                                <a href="/eventAdmin/setting/menu/project">Event Admin</a>
                                        </li>
                                        <%--
                                        <li>
                                                <a href="/anotherAdmin/setting/menu/another">Another Admin</a>
                                        </li>
                                        --%>
                                </ul>
                        </div>
                @endif

                @if(TemplateUtil.isEnableMainMenuUI() && !TemplateUtil.isAdminMenu())
                        <div class="drawer w-fit">
                                <input id="mainMenu-drawer" type="checkbox" class="drawer-toggle" />
                                <div class="drawer-content">
                                        <label for="mainMenu-drawer" class="btn btn-ghost btn-square h-14 drawer-button" aria-label="메인 메뉴">
                                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                                                        <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
                                                </svg>
                                        </label>
                                </div>
                                <div class="drawer-side z-30">
                                        <label for="mainMenu-drawer" aria-label="close sidebar" class="drawer-overlay"></label>
                                        <ul class="menu gap-1 p-4 w-64 min-h-full bg-base-100 border-r border-base-300">
                                                @for(var menu : TemplateUtil.getMainMenuList())
                                                        @if(menu.isDisplay())
                                                                <li>
                                                                        <a href="${menu.getUrl()}" aria-current="${navibarTitle.equals(menu.getName()) ? "page" : null}">${menu.getName()}</a>
                                                                </li>
                                                        @endif
                                                @endfor
                                        </ul>
                                </div>
                        </div>
                @endif

                <div class="hidden lg:flex items-center gap-2 px-4">
                        <span class="text-base font-medium">${navibarTitle}</span>
                        <span class="badge badge-sm badge-ghost font-mono">${env.getProperty("bluesky-boot-profile")}</span>
                </div>
        </div>

        <nav class="navbar-center">
                @template._components.navmenu(classAppend = "menu-horizontal")
                <div class="dropdown lg:hidden">
                        <label tabindex="0" class="btn btn-ghost btn-square h-14" aria-label="하위 메뉴">
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
                                </svg>
                        </label>
                        @template._components.navmenu(classAppend = "w-52 p-2 menu-sm dropdown-content z-30 bg-base-100 border border-base-300", isDropdown = true)
                </div>
        </nav>

        <div class="navbar-end gap-1 pr-2">
                !{String username = UserUtil.getUsername();}
                @if(username != null)
                        <div class="dropdown dropdown-end">
                                <div tabindex="0" role="button" class="btn btn-ghost btn-sm gap-2">
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                                                <path stroke-linecap="round" stroke-linejoin="round" d="M17.982 18.725A7.488 7.488 0 0 0 12 15.75a7.488 7.488 0 0 0-5.982 2.975m11.963 0a9 9 0 1 0-11.963 0m11.963 0A8.966 8.966 0 0 1 12 21a8.966 8.966 0 0 1-5.982-2.275M15 9.75a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                                        </svg>
                                        <span class="hidden lg:inline font-normal">${username}</span>
                                </div>
                                <ul tabindex="0" class="w-44 mt-2 p-2 menu menu-sm dropdown-content z-30 bg-base-100 border border-base-300">
                                        <li>
                                                <a href="/logout">
                                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                                                                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15m3 0 3-3m0 0-3-3m3 3H9" />
                                                        </svg>
                                                        로그아웃
                                                </a>
                                        </li>
                                </ul>
                        </div>
                @else
                        <a class="btn btn-ghost btn-sm" href="/oauth2/authorization/github">GitHub 로그인</a>
                        <a class="btn btn-ghost btn-sm" href="/oauth2/authorization/github-local2">GitHub local 로그인</a>
                @endif

                <button class="btn btn-ghost btn-sm btn-square" data-toggle-theme="bluesky-dark,bluesky" aria-label="테마 전환" title="테마 전환">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M21.752 15.002A9.72 9.72 0 0 1 18 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 0 0 3 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 0 0 9.002-5.998Z" />
                        </svg>
                </button>
        </div>
</header>
<div class="h-14"></div>

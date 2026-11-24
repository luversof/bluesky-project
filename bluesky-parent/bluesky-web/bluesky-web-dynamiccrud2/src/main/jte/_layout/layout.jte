@import net.luversof.client.user.util.UserUtil
@import net.luversof.client.user.config.ClientUserProperties
@import io.github.luversof.boot.context.ApplicationContextUtil

@param String title
@param gg.jte.Content content
@param gg.jte.Content sidebar = null
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null
@param String brand = null
@param String brandUrl = "/"
<%-- brandMenu: brand 자리에 대신 보일 것(사용 화면의 프로젝트 고르기, _components/projectSwitcher) --%>
@param gg.jte.Content brandMenu = null
@param boolean showUser = true
@param gg.jte.Content titleActions = null
<%-- stickyHead: 사이드바 화면에서 제목 아래, 제목과 함께 위에 붙어 있을 것(사용 화면의 검색 폼) --%>
@param gg.jte.Content stickyHead = null

!{var loggedIn = UserUtil.isAuthenticated();}
<%-- 관리 링크는 관리 권한이 있는 사용자에게만(메뉴 목록은 MenuPageService.visibleTree 가 메뉴 권한으로 거른다) --%>
!{var canManage = loggedIn && ApplicationContextUtil.getApplicationContext().getBean(net.luversof.web.dynamiccrud2.security.MenuAuthorizer.class).canManageSettings();}
!{var loginUrl =ApplicationContextUtil.getApplicationContext().getBean(ClientUserProperties.class).getLoginUrl();}
<%-- 지금 구역: /admin 아래면 관리, 그 밖(오류 화면 제외)은 사용 화면 --%>
!{var requestAttributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();}
!{var path = requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest().getRequestURI() : "";}
!{var section = !showUser ? null : path.startsWith("/admin") ? "admin" : "menu";}

<!DOCTYPE html>
<html lang="${net.luversof.web.dynamiccrud2.config.Msg.locale().getLanguage()}">
<head>
	<meta charset="utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<title>${title} · Dynamic CRUD 2</title>
	<%-- 테마는 그리기 전에 정한다(깜빡임 방지): 고른 값 → 없으면 운영체제 설정 --%>
	<script>
		(() => {
			let theme = null;
			try { theme = localStorage.getItem('dc2-theme'); } catch (e) {}
			document.documentElement.dataset.theme = theme || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
		})();
	</script>
	<link href="/main.css" rel="stylesheet" />
	<link rel="icon" href="data:," />
	<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
	<script src="/js/htmx-errors.js" defer></script>
	<script src="/js/ui.js" defer></script>
	<script src="/js/toggle.js" defer></script>
	<script src="/js/reorder.js" defer></script>
</head>
<%-- 브라우저 스크립트(js/htmx-errors.js · ui.js)가 쓰는 문구. {0} 은 스크립트가 채운다(MessageFormat 을 거치지 않게 인자 없이 읽음) --%>
<body class="min-h-screen flex flex-col"
	data-msg-forbidden="${net.luversof.web.dynamiccrud2.config.Msg.t("js.forbidden")}" data-msg-failed="${net.luversof.web.dynamiccrud2.config.Msg.t("js.failed")}"
	data-msg-offline="${net.luversof.web.dynamiccrud2.config.Msg.t("js.offline")}" data-msg-pick="${net.luversof.web.dynamiccrud2.config.Msg.t("js.pick")}"
	data-msg-no-candidates="${net.luversof.web.dynamiccrud2.config.Msg.t("common.noCandidates")}">
	<header class="border-b border-base-300 bg-base-100">
		<nav class="px-4 md:px-6 min-h-14 py-2 flex flex-wrap items-center gap-x-4 gap-y-1 whitespace-nowrap">
			<a href="/" class="font-semibold">Dynamic CRUD 2</a>
			@if(brand != null)
				<span class="hint" aria-hidden="true">/</span>
				@if(brandMenu != null)
					${brandMenu}
				@else
					<a href="${brandUrl}" class="font-semibold hover:underline">${brand}</a>
				@endif
			@endif
			<span class="ml-auto flex items-center gap-3 text-sm">
				@template._components.appNav(current = section, showAdmin = canManage)
				<span class="h-5 border-l border-base-300" aria-hidden="true"></span>
				@template._components.localeToggle()
				@template._components.themeToggle()
				@if(!showUser)
				@elseif(loggedIn)
					<span class="hidden sm:inline">${UserUtil.getUsername()}</span>
					<form action="/logout" method="post">
						<button class="btn btn-sm btn-ghost">${net.luversof.web.dynamiccrud2.config.Msg.t("layout.logout")}</button>
					</form>
				@else
					<%-- 돌아올 주소는 지금 보는 화면(브라우저 주소) --%>
					<a id="login-link" class="btn btn-sm btn-ghost" href="${loginUrl}">${net.luversof.web.dynamiccrud2.config.Msg.t("layout.login")}</a>
					<script>
						document.getElementById('login-link').href = '${loginUrl}?redirectUrl=' + encodeURIComponent(location.href);
					</script>
				@endif
			</span>
		</nav>
	</header>
	@if(sidebar != null)
		<div class="grow w-full px-4 md:px-6 py-6 flex flex-col md:flex-row gap-6">
			<aside class="md:w-56 shrink-0" aria-label="${net.luversof.web.dynamiccrud2.config.Msg.t("layout.sidebar")}">${sidebar}</aside>
			<main class="grow min-w-0">
				<%-- 제목과 stickyHead(사용 화면의 검색)는 세로 스크롤해도 위에 붙어 있다(.sticky-head) --%>
				<div class="sticky-head">
					<h1 class="text-xl font-semibold ${stickyHead != null ? "mb-4" : ""}">${title}</h1>
					${stickyHead}
				</div>
				${content}
			</main>
		</div>
	@else
		<main class="grow w-full px-4 md:px-6 py-6">
			@if(breadcrumb != null)@template._components.breadcrumb(items = breadcrumb)@endif
			<%-- titleActions: 화면 제목 줄 오른쪽 끝 단추(화면 전체에 대한 동작 - 전체 설정 변경 이력, 메뉴의 변경 이력 · 복사 · 삭제) --%>
			<div class="flex flex-wrap items-center gap-2 mb-4">
				<h1 class="text-xl font-semibold">${title}</h1>
				@if(titleActions != null)<div class="ml-auto flex flex-wrap items-center gap-2">${titleActions}</div>@endif
			</div>
			${content}
		</main>
	@endif
</body>
</html>

@import net.luversof.client.user.util.UserUtil
@import net.luversof.client.user.config.ClientUserProperties
@import io.github.luversof.boot.context.ApplicationContextUtil

@param String title
@param gg.jte.Content content
@param gg.jte.Content sidebar = null
@param String brand = null
@param String brandUrl = "/"
@param boolean showUser = true

!{var loggedIn = UserUtil.isAuthenticated();}
!{var loginUrl = ApplicationContextUtil.getApplicationContext().getBean(ClientUserProperties.class).getLoginUrl();}

<!DOCTYPE html>
<html lang="ko">
<head>
	<meta charset="utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<title>${title} · Dynamic CRUD 2</title>
	<link href="/main.css" rel="stylesheet" />
	<link rel="icon" href="data:," />
	<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
	<script src="/js/htmx-errors.js" defer></script>
</head>
<body class="min-h-screen flex flex-col">
	<header class="border-b border-base-300 bg-base-100">
		<nav class="mx-auto max-w-7xl px-4 min-h-14 py-2 flex flex-wrap items-center gap-x-4 gap-y-1 whitespace-nowrap">
			<a href="/" class="font-semibold">Dynamic CRUD 2</a>
			@if(brand != null)
				<span class="text-base-content/40">/</span>
				<a href="${brandUrl}" class="font-semibold hover:underline">${brand}</a>
			@endif
			<a href="/" class="text-sm hover:underline">메뉴</a>
			<a href="/admin" class="text-sm hover:underline">관리</a>
			<span class="ml-auto flex items-center gap-3 text-sm">
				@if(!showUser)
				@elseif(loggedIn)
					<span class="hidden sm:inline">${UserUtil.getUsername()}</span>
					<form action="/logout" method="post">
						<button class="btn btn-sm btn-ghost">로그아웃</button>
					</form>
				@else
					<%-- 돌아올 주소는 지금 보는 화면(브라우저 주소) --%>
					<a id="login-link" class="btn btn-sm btn-ghost" href="${loginUrl}">로그인</a>
					<script>
						document.getElementById('login-link').href = '${loginUrl}?redirectUrl=' + encodeURIComponent(location.href);
					</script>
				@endif
			</span>
		</nav>
	</header>
	@if(sidebar != null)
		<div class="grow mx-auto w-full max-w-7xl px-4 py-6 flex flex-col md:flex-row gap-6">
			<aside class="md:w-56 shrink-0" aria-label="메뉴">${sidebar}</aside>
			<main class="grow min-w-0">
				<h1 class="text-xl font-semibold mb-4">${title}</h1>
				${content}
			</main>
		</div>
	@else
		<main class="grow mx-auto w-full max-w-7xl px-4 py-6">
			<h1 class="text-xl font-semibold mb-4">${title}</h1>
			${content}
		</main>
	@endif
</body>
</html>

@param gg.jte.Content content
@param String title = "Bluesky"
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean hideHamburger = false

<!DOCTYPE html>
<html lang="ko" data-theme="light">
<head>
	<title>${title}</title>
	<script>
		(function() {
			var t = localStorage.getItem('theme'); if (t) document.documentElement.setAttribute('data-theme', t);
			if (localStorage.getItem('hideAmounts') === 'true') document.documentElement.classList.add('hide-amounts');
		})();
	</script>
	<link href="/main.css" rel="stylesheet" />
	<script src="https://unpkg.com/theme-change"></script>
	<script src="https://unpkg.com/htmx.org"></script>
	<script src="https://unpkg.com/htmx-ext-json-enc/json-enc.js"></script>
	<script src="/js/common.js"></script>
	<script src="/js/date-range-picker.js"></script>
</head>

<body>
	<div id="app-config" data-login-url="/login"></div>

	@template._components.body.navbar.navbar(isAuthenticated = isAuthenticated, username = username, loginUrl = loginUrl, profile = profile, hideHamburger = hideHamburger)

	${content}
	
	@template._components.body.footer()

</body>
</html>
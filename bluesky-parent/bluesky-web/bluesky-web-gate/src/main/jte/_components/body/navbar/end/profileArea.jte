@import net.luversof.web.gate.util.CspNonceHolder
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"

@if(isAuthenticated)
	<div>
		<div class="dropdown dropdown-end">
			<div tabindex="0" class="btn btn-ghost max-sm:px-2">
				@template._components.icons.profile()
				<div class="max-sm:hidden whitespace-nowrap">${username}</div>
			</div>
			<ul tabindex="0" class="menu menu-sm dropdown-content bg-base-100 rounded-box z-1 mt-3 w-52 p-2 shadow">
				<li>
					<form action="/logout" method="post">
						<button type="submit" class="flex items-center gap-1">
							@template._components.icons.logout() 로그아웃
						</button>
					</form>
				</li>
			</ul>
		</div>
	</div>
@else
	<a class="btn btn-ghost" id="login-link" href="#">
		@template._components.icons.login() 로그인
	</a>
	<script nonce="${CspNonceHolder.getNonce()}">
	document.addEventListener('DOMContentLoaded', function() {
		var loginLink = document.getElementById('login-link');
		if (loginLink) {
			var loginBase = '${loginUrl}';
			var redirectUrl = window.location.href;
			loginLink.href = loginBase + '?redirectUrl=' + encodeURIComponent(redirectUrl);
		}
	});
	</script>
@endif
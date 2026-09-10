@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil

!{
	String currentLocale = org.springframework.context.i18n.LocaleContextHolder.getLocale().toString();
}


<script nonce="${CspNonceHolder.getNonce()}">
	window.blueskyChangeLocale = window.blueskyChangeLocale || function(localeCode) {
		var search = window.location.search || '';
		if (search) {
			search = search.replace(/([?&])locale=[^&]*/i, '$1').replace(/[?&]$/, '');
		}
		var normalizedSearch = search && search !== '?' ? search : '';
		var separator = normalizedSearch ? '&' : '?';
		var target = window.location.pathname + normalizedSearch + separator + 'locale=' + encodeURIComponent(localeCode) + (window.location.hash || '');
		window.location.assign(target);
	};

	if (!window.__blueskyLocaleBound) {
		window.__blueskyLocaleBound = true;
		document.addEventListener('click', function(event) {
			var button = event.target && event.target.closest ? event.target.closest('[data-locale-code]') : null;
			if (button) {
				window.blueskyChangeLocale(button.dataset.localeCode);
			}
		});
	}
</script>

<div title="${MessageUtil.getMessage("common.label.locale")}" class="dropdown dropdown-end">
	<%-- 좁은 화면에서 글자가 숨으면 이름이 없어진다(실측 2026-09-09) - aria-label 은 항상, 드롭다운 트리거는 role/aria-haspopup. --%>
	<div tabindex="0" role="button" aria-haspopup="menu" aria-label="${MessageUtil.getMessage("common.label.locale")}" class="btn btn-ghost gap-2 max-sm:px-2">
		<span class="max-xl:hidden whitespace-nowrap">${MessageUtil.getMessage("common.label.locale")}</span>
		<span class="badge badge-ghost badge-sm">${currentLocale}</span>
		<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-3 h-3">
			<path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
		</svg>
	</div>
	<%-- 메뉴 상자 자체에는 tabindex 를 두지 않는다: 항목이 포커스를 받으면 focus-within 으로 열린 채 유지되고, 상자의 tabindex 는 빈 탭 정지만 만든다(실측 2026-09-10). --%>

	<div class="dropdown-content z-[1] bg-base-200 top-px w-48 overflow-y-auto mt-16 rounded-box shadow">
		<div class="grid gap-1 p-2">
			<button
				type="button"
				class="btn btn-sm btn-ghost justify-between"
				data-locale-code="ko_KR">
				<span>${MessageUtil.getMessage("common.locale.korean")}</span>
				<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="${"ko_KR".equals(currentLocale) ? "visible" : "invisible"} h-3 w-3 shrink-0">
					<path d="M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z" />
				</svg>
			</button>
			<button
				type="button"
				class="btn btn-sm btn-ghost justify-between"
				data-locale-code="en_US">
				<span>${MessageUtil.getMessage("common.locale.english.us")}</span>
				<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="${"en_US".equals(currentLocale) ? "visible" : "invisible"} h-3 w-3 shrink-0">
					<path d="M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z" />
				</svg>
			</button>
		</div>
	</div>
</div>
@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeFoulbornGroup
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeFoulbornGroup> groups = java.util.List.of()
@param java.util.Map<String, Integer> categories = java.util.Map.of()
@param String activeCategory = ""
@param String query = ""

!{
	String pageTitle = MessageUtil.getMessage("layout.menu.poe.foulborn");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	boolean ko = PoeText.isKorean();
	int modCount = groups.stream().mapToInt(g -> g.mods().size()).sum();
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				<span class="badge badge-ghost">${patchLabel}</span>
				<span class="text-sm text-base-content/50">${ko
					? "삿된 오브로 유니크의 기존 모드를 대체할 때 붙는 모드"
					: "Mods that replace a unique's existing mod when using Foulborn currency"}</span>
				<span class="text-xs font-mono text-base-content/40">${groups.size()} / ${modCount}</span>
			</div>

			<%-- ⚠ 이름이 아니라 토큰을 보여준다. 원본 데이터에 "이 토큰이 어느 유니크인지" 매핑이 없어서
			     (Jewel85 같은 GGG 내부 식별자만 존재) 추측으로 이름을 붙이지 않는다. 사용자가 오해하지 않게
			     화면에서도 그 사실을 밝힌다 — 조용히 토큰만 띄우면 "왜 이름이 없지?" 가 된다. --%>
			<div class="rounded-2xl border border-warning/30 bg-warning/5 px-3 py-2 text-xs text-base-content/70">
				${ko
					? "아이템별로 붙을 수 있는 삿된 옵션입니다. 이름을 못 찾은 소수는 게임 내부 식별자(예: HelmetDex2)로 남겨 뒀습니다 — 추측해서 붙이지 않습니다."
					: "Foulborn options per unique item. A few could not be resolved to a name and keep the game's internal id (e.g. HelmetDex2) — names are never guessed."}
			</div>

			<form class="flex flex-wrap items-center gap-1.5 rounded-2xl bg-base-100 p-3 shadow-sm" method="get" action="/poe/foulborn">
				<input id="poeFoulbornSearch" name="q" type="search" value="${query}"
				       class="input input-sm input-bordered w-full sm:w-72"
				       placeholder="${ko ? "아이템명·모드 검색 (예: 붉은 꿈, 생명력)" : "Search item name or mod text"}" autocomplete="off">
				<button class="btn btn-sm btn-primary" type="submit">${ko ? "검색" : "Search"}</button>
				<span class="text-xs font-mono text-base-content/50">${groups.size()}</span>
			</form>

			<%-- 분류 칩 — 모드 페이지 클래스 칩과 같은 관례(선택 표시 + 개수 소자) --%>
			<div class="flex flex-wrap items-center gap-1.5">
				<a class="badge ${activeCategory == null || activeCategory.isBlank() ? "badge-primary" : "badge-ghost"} badge-sm"
				   href="/poe/foulborn${query == null || query.isBlank() ? "" : "?q=" + query}">${ko ? "전체" : "All"}</a>
				@for(java.util.Map.Entry<String, Integer> entry : categories.entrySet())
					<a class="badge ${entry.getKey().equals(activeCategory) ? "badge-primary" : "badge-ghost"} badge-sm"
					   href="/poe/foulborn?category=${entry.getKey()}${query == null || query.isBlank() ? "" : "&q=" + query}">
						${entry.getKey()}<span class="ml-1 opacity-60">${entry.getValue()}</span></a>
				@endfor
			</div>

			@if(groups.isEmpty())
				<div class="rounded-2xl bg-base-100 p-6 text-center text-sm text-base-content/60 shadow-sm">
					${ko ? "결과가 없습니다. 데이터 갱신을 한 번도 안 돌렸다면 관리 화면에서 실행하세요." : "No results. Run the data update if you have not yet."}
				</div>
			@else
				<div class="grid gap-2 md:grid-cols-2 [&>*]:min-w-0">
					@for(PoeFoulbornGroup group : groups)
						<div class="rounded-2xl bg-base-100 p-3 shadow-sm">
							<div class="mb-1 flex flex-wrap items-baseline gap-2">
								<span class="badge badge-outline badge-sm border-fuchsia-500/40 text-fuchsia-500">${group.categoryKo()}</span>
								<%-- 이름이 있으면 그 유니크 상세로 — 삿된은 결국 "그 아이템" 이야기다 --%>
								@if(group.uniqueName() != null)
									<%-- 이름에 호버하면 **원본 유니크의 인게임 아이템 레이어**가 뜬다(젬·고유 목록과 같은 관례).
									     삿된은 그 아이템의 옵션이라, 원본이 어떤 물건인지 바로 볼 수 있어야 고를 수 있다. --%>
									<%-- JTE 스마트 속성: 값이 null 이면 속성 자체가 안 나간다(속성 자리에 @if 는 금지). --%>
									<a class="poe-name-unique font-semibold link-hover ${group.uniqueSlug() != null ? "poe-hover cursor-pointer" : ""}"
									   href="${group.uniqueSlug() != null ? "/poe/uniques/" + group.uniqueSlug() : "/poe/uniques?q=" + group.uniqueName()}"
									   hx-get="${group.uniqueSlug() != null ? "/poe/htmx/uniques/detail?slug=" + group.uniqueSlug() : null}"
									   hx-target="${group.uniqueSlug() != null ? "#poePreview" : null}"
									   hx-trigger="${group.uniqueSlug() != null ? "mouseenter delay:100ms" : null}"
									   hx-swap="${group.uniqueSlug() != null ? "innerHTML" : null}">${PoeText.name(group.uniqueNameKo(), group.uniqueName())}</a>
									@if(ko && group.uniqueNameKo() != null)<span class="text-xs text-base-content/40">${group.uniqueName()}</span>@endif
								@else
									<span class="font-mono text-sm text-base-content/60">${group.token()}</span>
								@endif
								<span class="ml-auto text-xs font-mono text-base-content/40">${group.mods().size()}</span>
							</div>
							@template.poe.htmx.foulbornOptions(mods = group.mods())
						</div>
					@endfor
				</div>
			@endif
		</div>
	`)

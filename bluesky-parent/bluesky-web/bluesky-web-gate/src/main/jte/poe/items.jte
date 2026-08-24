@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeGroups
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param int totalCount = 0
@param List<PoeGroups.Group> classGroups = java.util.List.of()
@param String initialQ = ""
@param String activeValue = "all"

!{
	String pageTitle = MessageUtil.getMessage("poe.items.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String searchPlaceholder = MessageUtil.getMessage("poe.items.search.placeholder");
	String classAllLabel = MessageUtil.getMessage("poe.uniques.category.all");
	// 탭 전환 진입 시 초기 필터를 그대로 로드(URL 인코딩 — itemClass 키/검색어에 공백 포함 가능)
	String initialUrl = "/poe/htmx/items?q=" + java.net.URLEncoder.encode(initialQ, java.nio.charset.StandardCharsets.UTF_8)
		+ "&itemClass=" + java.net.URLEncoder.encode(activeValue, java.nio.charset.StandardCharsets.UTF_8);
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
			</div>

			@template.poe.itemTabs(activeTab = "items")

			<div class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<form id="poeItemFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe/htmx/items"
				      hx-target="#poeItemList"
				      hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change"
				      data-url-sync data-url-map="itemClass:slot">
					<input type="search" name="q" value="${initialQ}" class="input input-sm input-bordered w-full sm:w-72" placeholder="${searchPlaceholder}" autocomplete="off" autofocus>
					<input type="hidden" name="itemClass" value="${activeValue}">
					<input type="hidden" name="attr" value="all">
				</form>
				@template.poe.groupChips(groups = classGroups, field = "itemClass", target = "#poeItemFilter", groupLabelPrefix = "poe.itemgroup.", allLabel = classAllLabel, activeValue = activeValue)
				<%-- 방어구 속성 베이스 필터(힘=방어도/민첩=회피/지능=에너지보호막). 방어구 부위 선택 시에만 표시(아래 스크립트). --%>
				!{ boolean itemsKo = net.luversof.web.gate.poe.PoeText.isKorean(); String[][] attrChips = {{"all", "전체", "All"}, {"str", "힘", "STR"}, {"dex", "민첩", "DEX"}, {"int", "지능", "INT"}, {"strdex", "힘·민첩", "STR/DEX"}, {"strint", "힘·지능", "STR/INT"}, {"dexint", "민첩·지능", "DEX/INT"}, {"strdexint", "힘·민첩·지능", "STR/DEX/INT"}}; }
				<div id="poeItemAttrRow" class="flex flex-wrap gap-1.5 items-center border-t border-base-200 pt-2" style="display:none">
					<span class="text-xs font-semibold text-base-content/50 mr-1">${itemsKo ? "방어구 속성" : "Defence base"}</span>
					@for(String[] a : attrChips)
						<button type="button" data-attr="${a[0]}"
						        class="btn btn-xs ${"all".equals(a[0]) ? "btn-primary" : "btn-outline"}"
						        onclick="var f=document.getElementById('poeItemFilter'); f.querySelector('input[name=attr]').value=this.dataset.attr; this.parentElement.querySelectorAll('button[data-attr]').forEach(function(b){b.classList.remove('btn-primary');b.classList.add('btn-outline');}); this.classList.remove('btn-outline'); this.classList.add('btn-primary'); f.dispatchEvent(new Event('change', {bubbles:true}));">${itemsKo ? a[1] : a[2]}</button>
					@endfor
				</div>
			</div>

			<div id="poeItemList" hx-get="${initialUrl}" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>

			<div id="poeItemDetail"></div>
			<script src="/js/poe/filterUrl.js" defer></script>
				<%-- 방어구 속성 칩은 방어구 부위(투구/갑옷/장갑/장화/방패) 선택 시에만 노출. 다른 부위면 숨기고 attr 초기화. --%>
				<script nonce="${CspNonceHolder.getNonce()}">
					(function () {
						var ARMOUR = { Helmet: 1, "Body Armour": 1, Gloves: 1, Boots: 1, Shield: 1 };
						var form = document.getElementById("poeItemFilter");
						var row = document.getElementById("poeItemAttrRow");
						if (!form || !row) return;
						function sync() {
							var el = form.querySelector("input[name=itemClass]");
							var show = el && ARMOUR[el.value];
							row.style.display = show ? "flex" : "none";
							if (!show) {
								var a = form.querySelector("input[name=attr]");
								if (a && a.value !== "all") {
									a.value = "all";
									row.querySelectorAll("button[data-attr]").forEach(function (b) {
										var on = b.dataset.attr === "all";
										b.classList.toggle("btn-primary", on);
										b.classList.toggle("btn-outline", !on);
									});
								}
							}
						}
						document.addEventListener("click", function (e) { if (e.target.closest("[data-chip-group]")) setTimeout(sync, 0); });
						form.addEventListener("change", function () { setTimeout(sync, 0); });
						sync();
					})();
				</script>
		</div>
	`
)

@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param List<Poe2.ItemClass> classes = java.util.List.of()
@param String initialQ = ""
@param String activeClass = "all"

!{
	String pageTitle = MessageUtil.getMessage("poe2.items.title");
	java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
	String initialUrl = "/poe2/htmx/items?q=" + java.net.URLEncoder.encode(initialQ, utf8) + "&itemClass=" + java.net.URLEncoder.encode(activeClass, utf8);
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			@template.poe2.header(title = pageTitle, meta = meta)
			@template.poe2.itemTabs(active = "items")
			<div class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<form id="poe2ItemFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe2/htmx/items" hx-target="#poe2ItemList" hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change" data-url-sync="true">
					<input type="search" name="q" value="${initialQ}" autofocus class="input input-sm input-bordered w-full sm:w-72"
					       placeholder="${MessageUtil.getMessage("poe.items.search.placeholder")}" aria-label="${MessageUtil.getMessage("poe2.search.placeholder")}" autocomplete="off">
					<input type="hidden" name="itemClass" value="${activeClass}">
					<input type="hidden" name="attr" value="all">
				</form>
				@template.poe2.classChips(classes = classes, target = "#poe2ItemFilter", activeClass = activeClass)
				<%-- 방어구 속성 베이스 필터 — PoE1 베이스 목록(poe/items.jte)과 같은 칩·문구(10-01: PoE2 에 없었다). 힘=방어도 · 민첩=회피 · 지능=에너지 보호막.
				     방어구 부위(투구 · 갑옷 · 장갑 · 장화 · 방패)를 골랐을 때만 보인다(아래 스크립트). 거르는 건 서버(Poe2HtmxController.items) --%>
				!{ boolean itemsKo = PoeText.isKorean(); String[][] attrChips = {{"all", "전체", "All"}, {"str", "힘", "STR"}, {"dex", "민첩", "DEX"}, {"int", "지능", "INT"}, {"strdex", "힘·민첩", "STR/DEX"}, {"strint", "힘·지능", "STR/INT"}, {"dexint", "민첩·지능", "DEX/INT"}, {"strdexint", "힘·민첩·지능", "STR/DEX/INT"}}; }
				<div id="poe2ItemAttrRow" class="flex flex-wrap gap-1.5 items-center border-t border-base-200 pt-2" style="display:none" data-attr-row>
					<span class="text-xs font-semibold text-base-content/50 mr-1">${itemsKo ? "방어구 속성" : "Defence base"}</span>
					@for(String[] a : attrChips)
						<button type="button" data-attr="${a[0]}" class="btn btn-xs ${"all".equals(a[0]) ? "btn-primary" : "btn-outline"}">${itemsKo ? a[1] : a[2]}</button>
					@endfor
				</div>
			</div>
			<div id="poe2ItemList" hx-get="${initialUrl}" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10"><span class="loading loading-spinner loading-lg text-primary"></span></div>
			</div>
			<script src="/js/poe/filterUrl.js" defer></script>
			<script nonce="${CspNonceHolder.getNonce()}">
				(function () {
					var ARMOUR = { Helmet: 1, "Body Armour": 1, Gloves: 1, Boots: 1, Shield: 1 };
					var form = document.getElementById("poe2ItemFilter");
					var row = document.getElementById("poe2ItemAttrRow");
					if (!form || !row) return;
					var attr = form.querySelector("input[name=attr]");
					function mark(v) {
						row.querySelectorAll("button[data-attr]").forEach(function (b) {
							var on = b.dataset.attr === v;
							b.classList.toggle("btn-primary", on);
							b.classList.toggle("btn-outline", !on);
							b.setAttribute("aria-pressed", on ? "true" : "false");
						});
					}
					row.addEventListener("click", function (e) {
						var b = e.target.closest("button[data-attr]");
						if (!b) return;
						attr.value = b.dataset.attr;
						mark(attr.value);
						form.dispatchEvent(new Event("change", { bubbles: true }));
					});
					// 방어구가 아닌 부위로 바꾸면 숨기고 attr 를 되돌린다(무기에 "힘" 이 남아 빈 목록이 되지 않게)
					function sync() {
						var el = form.querySelector("input[name=itemClass]");
						var show = el && ARMOUR[el.value];
						row.style.display = show ? "flex" : "none";
						if (!show && attr.value !== "all") { attr.value = "all"; mark("all"); }
					}
					document.addEventListener("click", function (e) { if (e.target.closest("[data-chip-group]")) setTimeout(sync, 0); });
					form.addEventListener("change", function () { setTimeout(sync, 0); });
					mark("all");
					sync();
				})();
			</script>
		</div>
	`
)

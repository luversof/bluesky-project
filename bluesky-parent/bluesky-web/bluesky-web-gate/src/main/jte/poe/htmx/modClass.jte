@import net.luversof.web.gate.poe.dto.PoeModClass
@import net.luversof.web.gate.poe.dto.PoeModTier
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeModClass mods
@param net.luversof.web.gate.poe.dto.PoeEldritch eldritch = null
<%-- 속성 변형 탭 표시 여부 — 아이템 상세 페이지는 베이스가 변형을 결정하므로 끈다(바꿀 수 있으면 오해를 부른다) --%>
@param boolean showVariants = true
<%-- 영향력 탭이 걸 링크의 접두("…?"로 끝나는 형태). 지정하면 htmx 없이 그 페이지를 다시 연다(아이템 상세용) --%>
@param String influenceHref = null
@param java.util.List<net.luversof.web.gate.poe.dto.PoeAnointEntry> anoints = null
@param java.util.List<net.luversof.web.gate.poe.dto.PoeEssenceEntry> essences = null
@param java.util.List<net.luversof.web.gate.poe.dto.PoeBenchEntry> bench = null

!{
	boolean ko = PoeText.isKorean();
	// 한 패밀리의 스폰웨이트 합(정규화 기준 아님 — 티어별 상대 표기는 최상위 대비로 단순화)
}

@if(mods == null)
	<div class="p-6 text-center text-base-content/50">${ko ? "이 클래스의 모드 데이터가 없습니다." : "No mods for this class."}</div>
@else
	!{
		String clsEnc = java.net.URLEncoder.encode(mods.itemClass(), java.nio.charset.StandardCharsets.UTF_8);
	}
	<%-- 속성 변형 탭 — 방어구는 힘/민첩/지능 베이스마다 붙는 모드가 다르다(ES=지능, 방어도=힘, 회피=민첩) --%>
	@if(showVariants && !mods.variants().isEmpty())
		<div class="mb-2 flex flex-wrap items-center gap-1.5" data-mod-variants>
			<span class="text-sm text-base-content/60 mr-1">${ko ? "속성 변형" : "Attribute"}</span>
			@for(var v : mods.variants())
				!{ String url = "/poe/mods?itemClass=" + clsEnc + "&variant=" + v.key() + "&influence=" + mods.influence(); }
				<a class="poe-chip badge ${v.key().equals(mods.variant()) ? "badge-secondary" : "badge-ghost"} cursor-pointer"
				   href="${url}"
				   hx-get="/poe/htmx/mods?itemClass=${clsEnc}&variant=${v.key()}&influence=${mods.influence()}"
				   hx-target="#poeModBody" hx-swap="innerHTML" hx-push-url="${url}">
					${ko ? v.nameKo() : v.name()}
					<span class="ml-1 text-[10px] opacity-60">${v.prefixCount() + v.suffixCount()}</span>
				</a>
			@endfor
		</div>
	@endif

	<%-- 영향력 탭 — 쉐이퍼/엘더/정복자 4종. 영향력 아이템엔 전용 접두/접미가 추가로 붙는다(+N = 추가 계열 수) --%>
	@if(!mods.influences().isEmpty())
		<div class="mb-3 flex flex-wrap items-center gap-1.5" data-mod-influences>
			<span class="text-sm text-base-content/60 mr-1">${ko ? "영향력" : "Influence"}</span>
			@if(influenceHref != null)
				<%-- 아이템 상세: 그 아이템 페이지를 다시 여는 평범한 링크(모드 페이지로 이탈하지 않게) --%>
				<a class="poe-chip badge ${mods.influence().isEmpty() ? "badge-accent" : "badge-ghost"} cursor-pointer"
				   href="${influenceHref}influence=">${ko ? "없음" : "None"}</a>
				@for(var inf : mods.influences())
					<a class="poe-chip badge ${inf.key().equals(mods.influence()) ? "badge-accent" : "badge-ghost"} cursor-pointer"
					   href="${influenceHref}influence=${inf.key()}">
						${ko ? inf.nameKo() : inf.name()}
						<span class="ml-1 text-[10px] opacity-60">+${inf.extraCount()}</span>
					</a>
				@endfor
			@else
				!{ String noneUrl = "/poe/mods?itemClass=" + clsEnc + "&variant=" + mods.variant(); }
				<a class="poe-chip badge ${mods.influence().isEmpty() ? "badge-accent" : "badge-ghost"} cursor-pointer"
				   href="${noneUrl}"
				   hx-get="/poe/htmx/mods?itemClass=${clsEnc}&variant=${mods.variant()}"
				   hx-target="#poeModBody" hx-swap="innerHTML" hx-push-url="${noneUrl}">${ko ? "없음" : "None"}</a>
				@for(var inf : mods.influences())
					!{ String url = "/poe/mods?itemClass=" + clsEnc + "&variant=" + mods.variant() + "&influence=" + inf.key(); }
					<a class="poe-chip badge ${inf.key().equals(mods.influence()) ? "badge-accent" : "badge-ghost"} cursor-pointer"
					   href="${url}"
					   hx-get="/poe/htmx/mods?itemClass=${clsEnc}&variant=${mods.variant()}&influence=${inf.key()}"
					   hx-target="#poeModBody" hx-swap="innerHTML" hx-push-url="${url}">
						${ko ? inf.nameKo() : inf.name()}
						<span class="ml-1 text-[10px] opacity-60">+${inf.extraCount()}</span>
					</a>
				@endfor
			@endif
		</div>
	@endif

	<%-- 섹션 점프 네비 — 페이지가 길어져(접두/접미/부패/에센스/엘드리치/도유) poedb 처럼 바로가기 제공.
	     프래그먼트 안에 있어 htmx 전환에도 유지되고, 존재하는 섹션만 칩이 생긴다. --%>
	<div class="mb-3 flex flex-wrap items-center gap-1.5 text-xs">
		<a class="badge badge-ghost badge-sm" href="#mod-sec-affix">${ko ? "접두·접미" : "Affixes"}</a>
		@if(mods.corrupted() != null && !mods.corrupted().isEmpty())
			<a class="badge badge-ghost badge-sm" href="#mod-sec-corrupted">${ko ? "부패" : "Corrupted"}</a>
		@endif
		@if(mods.enchants() != null && !mods.enchants().isEmpty())
			<a class="badge badge-ghost badge-sm" href="#mod-sec-enchant">${ko ? "인챈트" : "Enchants"}</a>
		@endif
		@if(essences != null && !essences.isEmpty())
			<a class="badge badge-ghost badge-sm" href="#mod-sec-essence">${ko ? "에센스" : "Essences"}</a>
		@endif
		@if(bench != null && !bench.isEmpty())
			<a class="badge badge-ghost badge-sm" href="#mod-sec-bench">${ko ? "작업대" : "Bench"}</a>
		@endif
		@if(eldritch != null)
			<a class="badge badge-ghost badge-sm" href="#mod-sec-eldritch">${ko ? "엘드리치" : "Eldritch"}</a>
		@endif
		@if(anoints != null)
			<a class="badge badge-ghost badge-sm" href="#mod-sec-anoint">${ko ? "도유" : "Anoints"}</a>
		@endif
	</div>

	!{
		// 풀 총 가중치(접두/접미 분리) — Craft of Exile 식 "이 티어가 무작위 롤에서 뜰 확률"의 분모.
		// 확률 = 티어 가중치 / 그 젠(접두|접미) 풀 전체 가중치 합.
		long prefixPool = 0;
		for (PoeModClass.NamedFamily nf : mods.prefixes()) for (var t : nf.family().tiers()) prefixPool += t.weight();
		long suffixPool = 0;
		for (PoeModClass.NamedFamily nf : mods.suffixes()) for (var t : nf.family().tiers()) suffixPool += t.weight();
	}
	<div id="mod-sec-affix" class="grid gap-4 md:grid-cols-2 scroll-mt-20">
		<%-- 접두 --%>
		<div class="min-w-0">
			<h2 class="mb-2 flex items-baseline gap-2 font-bold text-[#8888ff]">
				${ko ? "접두" : "Prefixes"}
				<span class="text-xs text-base-content/40">${mods.prefixes().size()}</span>
			</h2>
			<div class="space-y-2">
				@for(PoeModClass.NamedFamily nf : mods.prefixes())
					@template.poe.htmx.modFamilyCard(key = nf.key(), family = nf.family(), ko = ko, poolWeight = prefixPool, selectable = true)
				@endfor
			</div>
		</div>
		<%-- 접미 --%>
		<div class="min-w-0">
			<h2 class="mb-2 flex items-baseline gap-2 font-bold text-[#8888ff]">
				${ko ? "접미" : "Suffixes"}
				<span class="text-xs text-base-content/40">${mods.suffixes().size()}</span>
			</h2>
			<div class="space-y-2">
				@for(PoeModClass.NamedFamily nf : mods.suffixes())
					@template.poe.htmx.modFamilyCard(key = nf.key(), family = nf.family(), ko = ko, poolWeight = suffixPool, selectable = true)
				@endfor
			</div>
		</div>
	</div>

	<%-- 제작 계산기 — 체크한 패밀리들의 "한 롤 동시 등장" 근사 확률(독립 가정, 자리 경쟁 미반영이라 실제보다 낙관).
	     craftofexile 의 정밀 시뮬 대체가 아니라 러프 비교용임을 라벨로 명시한다. --%>
	<div id="poeCraftCalc" class="sticky bottom-2 z-10 mt-3 hidden flex-wrap items-center gap-2 rounded-box border border-primary/30 bg-base-100 px-3 py-2 text-sm shadow-lg">
		<span class="font-semibold text-primary" id="poeCraftCalcLabel"></span>
		<span class="font-mono" id="poeCraftCalcValue"></span>
		<span class="text-[11px] text-base-content/40">${ko ? "독립 가정 근사 — 접두/접미 자리 경쟁 미반영(실제는 더 낮음)" : "independence approximation"}</span>
		<button type="button" class="btn btn-ghost btn-xs ml-auto" id="poeCraftCalcClear">${ko ? "선택 해제" : "Clear"}</button>
	</div>
	<script>
		(function () {
			var calc = document.getElementById("poeCraftCalc");
			if (!calc) return;
			var label = document.getElementById("poeCraftCalcLabel");
			var value = document.getElementById("poeCraftCalcValue");
			function update() {
				var picked = [].filter.call(document.querySelectorAll("#mod-sec-affix .poe-craft-pick, #mod-sec-bench .poe-craft-pick, #mod-sec-essence .poe-craft-pick"), function (c) { return c.checked; });
				if (!picked.length) { calc.classList.add("hidden"); calc.classList.remove("flex"); return; }
				var p = 1, prefixN = 0, suffixN = 0, benchN = 0, essenceN = 0;
				picked.forEach(function (c) {
					var card = c.closest("[data-family-odds]");
					var odds = parseFloat(card.getAttribute("data-family-odds")) / 100;
					p *= odds;
					if (card.getAttribute("data-family-gen") === "prefix") prefixN++; else suffixN++;
					if (card.closest("#mod-sec-bench")) benchN++;
					if (card.closest("#mod-sec-essence")) essenceN++;
				});
				calc.classList.remove("hidden"); calc.classList.add("flex");
				var en = document.documentElement.lang === "en";
				label.textContent = (en ? "Combined odds (" : "동시 확률 (") + picked.length + (en ? " mods, per roll" : "개, 1롤")
					+ (benchN ? (en ? " + " + benchN + " bench" : " + 벤치 " + benchN) : "")
					+ (essenceN ? (en ? " + essence" : " + 에센스") : "") + ")";
				// 에센스 제작은 한 번에 1개만 — 2개+ 선택은 불가(서로 다른 에센스를 동시에 쓸 수 없다)
				if (essenceN > 1) {
					value.textContent = en ? "impossible (one essence per craft)" : "불가 — 에센스는 1개만";
					return;
				}
				// 크래프트는 기본 1개. 2개는 멀티모드 전제 — 멀티모드 자체가 크래프트 접미 1칸을 차지하고
				// "크래프트 최대 3개"에 스스로 포함되므로 유효 크래프트 한도는 2개다(3개+ 선택은 불가).
				if (benchN > 2) {
					value.textContent = en
						? "impossible (multimod allows 2 crafts + itself)"
						: "불가 — 크래프트는 멀티모드 포함 3개(선택 가능 2개)";
					return;
				}
				if (benchN === 2) suffixN++; // 멀티모드(접미) 슬롯
				// 게임 규칙: 접두/접미 각 최대 3(벤치 크래프트·멀티모드도 슬롯을 차지) — 초과 조합은 확률이 아니라 불가
				if (prefixN > 3 || suffixN > 3) {
					var multiNote = benchN === 2 && suffixN > 3 ? (en ? " (incl. multimod suffix)" : " (멀티모드 접미 포함)") : "";
					value.textContent = en
						? "impossible (max 3 " + (prefixN > 3 ? "prefixes" : "suffixes") + ")" + multiNote
						: "불가 — " + (prefixN > 3 ? "접두" : "접미") + " 3개 초과" + multiNote;
					return;
				}
				var pct = p * 100;
				value.textContent = (pct >= 0.01 ? pct.toFixed(2) : pct.toExponential(1)) + "% · " + (en ? "avg 1 in " : "평균 1/") + Math.round(1 / p).toLocaleString()
					+ (benchN === 2 ? (en ? " · assumes multimod (uses a suffix)" : " · 멀티모드 전제(접미 1칸)") : "");
			}
			document.addEventListener("change", function (e) {
				if (e.target && e.target.classList && e.target.classList.contains("poe-craft-pick")) update();
			});
			document.getElementById("poeCraftCalcClear").addEventListener("click", function () {
				document.querySelectorAll("#mod-sec-affix .poe-craft-pick, #mod-sec-bench .poe-craft-pick, #mod-sec-essence .poe-craft-pick").forEach(function (c) { c.checked = false; });
				update();
			});
		})();
	</script>

	<%-- 바알 오브 부패 임플리싯 — 부패 시 뜰 수 있는 임플리싯 풀(접두/접미와 별개, 영향력 무관).
	     확률 분모 = 이 클래스 부패 풀 전체 가중치 합(craftofexile 방식). --%>
	@if(mods.corrupted() != null && !mods.corrupted().isEmpty())
		!{
			long corruptedPool = 0;
			for (PoeModClass.NamedFamily nf : mods.corrupted()) for (var t : nf.family().tiers()) corruptedPool += t.weight();
		}
		<div id="mod-sec-corrupted" class="mt-4 rounded-2xl bg-base-100 p-4 shadow-sm scroll-mt-20">
			<div class="mb-2 flex flex-wrap items-baseline gap-2">
				<h2 class="font-bold text-red-500">${ko ? "바알 오브 부패 임플리싯" : "Corrupted implicits"}</h2>
				<span class="text-sm text-base-content/50">${ko ? "부패 시 뜰 수 있는 임플리싯 — 영향력과 무관" : "Possible implicits when corrupting — influence-independent"}</span>
				<span class="text-xs font-mono text-base-content/40">${mods.corrupted().size()}</span>
			</div>
			<div class="grid gap-2 md:grid-cols-2 [&>*]:min-w-0">
				@for(PoeModClass.NamedFamily nf : mods.corrupted())
					@template.poe.htmx.modFamilyCard(key = nf.key(), family = nf.family(), ko = ko, poolWeight = corruptedPool)
				@endfor
			</div>
		</div>
	@endif

	<%-- 플라스크 인챈트(주입/점화 오브) — 플라스크 클래스만. 접두/접미와 별개 슬롯, 확률 분모 = 젠(주입|점화)별 합 --%>
	@if(mods.enchants() != null && !mods.enchants().isEmpty())
		!{
			long enchantPool = 0;
			for (PoeModClass.NamedFamily nf : mods.enchants()) for (var t : nf.family().tiers()) enchantPool += t.weight();
		}
		<div id="mod-sec-enchant" class="mt-4 rounded-2xl bg-base-100 p-4 shadow-sm scroll-mt-20">
			<div class="mb-2 flex flex-wrap items-baseline gap-2">
				<h2 class="font-bold text-violet-500">${ko ? "플라스크 인챈트" : "Flask enchants"}</h2>
				<span class="text-sm text-base-content/50">${ko ? "주입 오브(사용 조건)·점화 오브(강화) — 오브당 1개 부여" : "Instilling + Enkindling orbs"}</span>
				<span class="text-xs font-mono text-base-content/40">${mods.enchants().size()}</span>
			</div>
			<div class="grid gap-2 md:grid-cols-2 [&>*]:min-w-0">
				@for(PoeModClass.NamedFamily nf : mods.enchants())
					@template.poe.htmx.modFamilyCard(key = nf.key(), family = nf.family(), ko = ko, poolWeight = enchantPool)
				@endfor
			</div>
		</div>
	@endif

	<%-- 엘드리치 임플리싯(총주교/포식자) — 대상 슬롯일 때만. 제작 접두/접미와 별개(화폐로 붙이는 임플리싯) --%>
	@if(eldritch != null)
		<div id="mod-sec-eldritch" class="mt-4 rounded-2xl bg-base-100 p-4 shadow-sm scroll-mt-20">
			<div class="mb-2 flex flex-wrap items-baseline gap-2">
				<h2 class="font-bold text-amber-500">${ko ? "엘드리치 임플리싯" : "Eldritch implicits"}</h2>
				<span class="text-sm text-base-content/50">${ko ? "총주교·포식자 화폐로 부여 — 팩션당 1개씩 최대 2개" : "Exarch + Eater — up to one each"}</span>
			</div>
			@template.poe.htmx.eldritchPools(eldritch = eldritch, ko = ko)
		</div>
	@endif

	<%-- 에센스 — "이 에센스를 쓰면 이 클래스에 이 모드가 보장"(poedb/craftofexile 식). 계열별 접이식, 티어 내림차순.
	     data-mod-family 훅으로 페이지 검색 필터에 합류(티어 행 없음 → 레벨 필터 비대상, #94 규칙). --%>
	@if(essences != null && !essences.isEmpty())
		!{
			// family 키 순서 유지하며 그룹화(파서가 계열별·티어 내림차순으로 정렬해 둠)
			var essFamilies = new java.util.LinkedHashMap<String, java.util.List<net.luversof.web.gate.poe.dto.PoeEssenceEntry>>();
			for (var e : essences) essFamilies.computeIfAbsent(e.family(), k -> new java.util.ArrayList<>()).add(e);
		}
		<div id="mod-sec-essence" class="mt-4 rounded-2xl bg-base-100 p-4 shadow-sm scroll-mt-20">
			<div class="mb-2 flex flex-wrap items-baseline gap-2">
				<h2 class="font-bold text-teal-500">${ko ? "에센스" : "Essences"}</h2>
				<span class="text-sm text-base-content/50">${ko ? "에센스로 제작 시 이 모드가 보장 — 계열별 상위 티어 먼저" : "Guaranteed mod when crafting with an essence"}</span>
				<span class="text-xs font-mono text-base-content/40">${essFamilies.size()}</span>
			</div>
			<div class="grid gap-2 md:grid-cols-2 [&>*]:min-w-0">
				@for(var fam : essFamilies.entrySet())
					!{ var topEss = fam.getValue().get(0); }
					<%-- 계산기 참여: 에센스 제작은 이 모드 확정(100%) — 확률 곱 중립, 젠 슬롯은 차지. 에센스는 1개만 가능(스크립트 가드) --%>
					<details class="rounded-lg border border-base-200 bg-base-100 overflow-hidden" data-mod-family="essence-${fam.getKey()}" data-family-odds="100" data-family-gen="${topEss.gen()}">
						<summary class="cursor-pointer select-none px-3 py-2 hover:bg-base-200/50 list-none flex items-center gap-2">
							<input type="checkbox" class="checkbox checkbox-xs poe-craft-pick shrink-0" onclick="event.stopPropagation()" title="${ko ? "동시 확률 계산에 에센스 확정으로 포함" : "Include as essence-guaranteed"}">
							@if(topEss.icon() != null)
								<img src="/poe-assets/${topEss.icon()}" class="w-6 h-6 object-contain shrink-0" alt="${ko ? topEss.nameKo() : topEss.name()}" loading="lazy">
							@endif
							<span class="text-xs text-base-content/30 font-mono shrink-0">${fam.getValue().size()}T</span>
							<span class="text-[10px] font-mono shrink-0 ${"prefix".equals(topEss.gen()) ? "text-[#8888ff]" : "text-amber-400"}">${"prefix".equals(topEss.gen()) ? (ko ? "접두" : "pre") : (ko ? "접미" : "suf")}</span>
							<span class="min-w-0 flex-1">
								@for(String line : (ko ? topEss.ko() : topEss.en()))
									<span class="text-[13px] text-teal-400 block truncate">$unsafe{PoeText.highlightValues(line)}</span>
								@endfor
							</span>
							<span class="text-[10px] text-base-content/25 shrink-0">${fam.getKey()}</span>
						</summary>
						<div class="border-t border-base-200 px-3 py-1.5 space-y-1">
							@for(var e : fam.getValue())
								<div class="flex items-center gap-2 text-[12px]">
									@if(e.icon() != null)
										<img src="/poe-assets/${e.icon()}" class="w-5 h-5 object-contain shrink-0" alt="" loading="lazy">
									@endif
									<span class="shrink-0 font-mono text-base-content/40 w-6" title="${ko ? "에센스 티어" : "Essence tier"}">T${e.tier()}</span>
									<span class="shrink-0 min-w-0 max-w-[45%] truncate text-base-content/60">${ko ? e.nameKo() : e.name()}</span>
									<span class="min-w-0 whitespace-pre-line">
										!{java.util.List<String> mx = ko ? e.ko() : e.en();}
										!{java.util.List<String> mn = ko ? e.koMin() : e.enMin();}
										@for(int i = 0; i < mx.size(); i++)
											$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)))}@if(i < mx.size() - 1), @endif
										@endfor
									</span>
								</div>
							@endfor
						</div>
					</details>
				@endfor
			</div>
		</div>
	@endif

	<%-- 장인 작업대(벤치크래프트) — 화폐를 내고 확정 부여하는 모드(poedb/craftofexile 의 Crafting Bench 식).
	     티어 행 없는 카드 → 레벨 필터 비대상(#94 규칙), data-mod-family 로 검색 필터 합류. --%>
	@if(bench != null && !bench.isEmpty())
		!{
			// 계열(첫 스탯줄 어간) 순서 유지 그룹화 — 파서가 접두→접미·계열·티어 순으로 정렬해 둠
			var benchFamilies = new java.util.LinkedHashMap<String, java.util.List<net.luversof.web.gate.poe.dto.PoeBenchEntry>>();
			for (var b : bench) {
				String benchKey = b.gen() + "|" + (b.en().isEmpty() ? "?" : b.en().get(0).replaceAll("[0-9.()+%–-]+", ""));
				benchFamilies.computeIfAbsent(benchKey, k -> new java.util.ArrayList<>()).add(b);
			}
		}
		<div id="mod-sec-bench" class="mt-4 rounded-2xl bg-base-100 p-4 shadow-sm scroll-mt-20">
			<div class="mb-2 flex flex-wrap items-baseline gap-2">
				<h2 class="font-bold text-lime-500">${ko ? "장인 작업대" : "Crafting bench"}</h2>
				<span class="text-sm text-base-content/50">${ko ? "화폐를 내고 확정 부여 — 계열별 상위 티어 먼저, 비용 표기" : "Deterministic crafts for currency"}</span>
				<span class="text-xs font-mono text-base-content/40">${benchFamilies.size()}</span>
			</div>
			<div class="grid gap-2 md:grid-cols-2 [&>*]:min-w-0">
				@for(var fam : benchFamilies.entrySet())
					!{ var topBench = fam.getValue().get(0); }
					<%-- 계산기 참여: 벤치는 확정(100%)이라 확률 곱엔 중립이지만 접두/접미 슬롯은 차지한다(젠 상한 가드 대상) --%>
					<details class="rounded-lg border border-base-200 bg-base-100 overflow-hidden" data-mod-family="bench-${fam.getKey().hashCode()}" data-family-odds="100" data-family-gen="${topBench.gen()}">
						<summary class="cursor-pointer select-none px-3 py-2 hover:bg-base-200/50 list-none flex items-center gap-2">
							<input type="checkbox" class="checkbox checkbox-xs poe-craft-pick shrink-0" onclick="event.stopPropagation()" title="${ko ? "동시 확률 계산에 확정 크래프트로 포함" : "Include as deterministic craft"}">
							<span class="text-xs text-base-content/30 font-mono shrink-0">${fam.getValue().size()}T</span>
							<span class="text-[10px] font-mono shrink-0 ${"prefix".equals(topBench.gen()) ? "text-[#8888ff]" : "text-amber-400"}">${"prefix".equals(topBench.gen()) ? (ko ? "접두" : "pre") : (ko ? "접미" : "suf")}</span>
							<span class="min-w-0 flex-1">
								@for(String line : (ko ? topBench.ko() : topBench.en()))
									<span class="text-[13px] text-lime-400 block truncate">$unsafe{PoeText.highlightValues(line)}</span>
								@endfor
							</span>
						</summary>
						<div class="border-t border-base-200 px-3 py-1.5 space-y-1">
							@for(var b : fam.getValue())
								<div class="flex items-center gap-2 text-[12px]">
									<span class="shrink-0 font-mono text-base-content/40 w-6" title="${ko ? "벤치 티어" : "Bench tier"}">T${b.tier()}</span>
									<span class="min-w-0 flex-1 whitespace-pre-line">
										!{java.util.List<String> mx = ko ? b.ko() : b.en();}
										!{java.util.List<String> mn = ko ? b.koMin() : b.enMin();}
										@for(int i = 0; i < mx.size(); i++)
											$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)))}@if(i < mx.size() - 1), @endif
										@endfor
									</span>
									@if(b.cost() != null && !b.cost().isEmpty())
										<span class="shrink-0 inline-flex items-center gap-1 text-[11px] text-base-content/50">
											@for(var c : b.cost())${c.count()}×@if(c.icon() != null)<img src="/poe-assets/${c.icon()}" class="w-4 h-4 object-contain inline-block" alt="${ko ? c.nameKo() : c.name()}" title="${ko ? c.nameKo() : c.name()}" loading="lazy" onerror="this.replaceWith('${ko ? c.nameKo() : c.name()}')">@else ${ko ? c.nameKo() : c.name()}@endif @endfor
										</span>
									@endif
								</div>
							@endfor
						</div>
					</details>
				@endfor
			</div>
		</div>
	@endif

	<%-- 속성 부여(도유) — 아뮬렛 전용. 프래그먼트 안에 둬야 ① htmx 클래스 전환에도 나타나고
	     ② 페이지 검색 필터(scope 내 data-mod-family)가 그대로 걸린다. --%>
	@if(anoints != null)
		<div id="mod-sec-anoint" class="mt-4 rounded-2xl bg-base-100 p-4 shadow-sm scroll-mt-20">
			<div class="mb-2 flex flex-wrap items-baseline gap-2">
				<h2 class="font-bold text-amber-500">${ko ? "속성 부여 (도유)" : "Anointments"}</h2>
				<span class="text-sm text-base-content/50">${ko ? "성유 3개로 아뮬렛에 노터블 부여 — 비싼 성유 우선 정렬, 검색 필터 적용" : "3 oils grant a notable on an amulet"}</span>
				<span class="text-xs font-mono text-base-content/40">${anoints.size()}</span>
			</div>
			@template.poe.anointList(anoints = anoints, ko = ko)
		</div>
	@endif
@endif

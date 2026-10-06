@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.PoeText

@param PoeUniqueItem item
@param PoeBaseItem base = null
@param Integer variant = null
@param boolean oob = false

<%-- 변형 선택 + 아이템 레이어. 임프레션스(물리/화염/냉기/번개/카오스)처럼 **인게임에 동시에 존재하는**
     서로 다른 아이템들이라, 하나만 보여주면 나머지는 없는 셈이 된다. 칩으로 갈아끼운다.
     라벨은 게임에 없는 우리 쪽 장치라 툴팁 밖에 두고, 툴팁 자체는 인게임 그대로 유지한다. --%>
<div id="uniqueVariantBox" class="shrink-0 space-y-2">
	@if(item.variants() != null && !item.variants().isEmpty())
		!{ int current = variant != null ? variant.intValue() : (item.defaultVariant() != null ? item.defaultVariant().intValue() : item.variants().get(0).index()); }
		@if(item.variants().size() > 24)
			<%-- 변형이 많은 고유(소아사의 진주 656 · 금단의 샤코 406 · 거대망상 300 — PoB 생성 고유, 10-03 C76)는 칩을 다 늘어놓으면 높이 6,908px 벽이 된다(10-04 C80).
			     select 로 줄인다: 글자 입력으로 찾기는 브라우저 기본, 스크립트가 없어도 폼 제출로 동작, htmx 가 있으면 이 조각만 교체(주소는 응답 머리 HX-Push-Url). --%>
			<form class="flex w-80 max-w-[calc(100vw-2rem)] flex-wrap items-center gap-1" method="get" action="/poe/uniques/${item.slug()}">
				<label class="text-xs text-base-content/60" for="uniqueVariantSelect">${PoeText.isKorean() ? "변형" : "Variant"} ${item.variants().size()}</label>
				<select id="uniqueVariantSelect" name="variant" class="select select-bordered select-sm min-w-0 flex-1"
				        hx-get="/poe/htmx/uniques/variant?slug=${item.slug()}" hx-trigger="change" hx-target="#uniqueVariantBox" hx-swap="outerHTML">
					@for(var v : item.variants())
						<option value="${v.index()}" selected="${v.index() == current}">${PoeText.name(v.nameKo(), v.name())}</option>
					@endfor
				</select>
				<noscript><button type="submit" class="btn btn-sm">${PoeText.isKorean() ? "보기" : "Show"}</button></noscript>
			</form>
		@else
		<div class="flex w-80 max-w-[calc(100vw-2rem)] flex-wrap items-center gap-1">
			<span class="text-xs text-base-content/60">${PoeText.isKorean() ? "변형" : "Variant"}</span>
			@for(var v : item.variants())
				<a class="badge badge-sm cursor-pointer ${v.index() == current ? "badge-primary" : "badge-ghost"}"
				   href="/poe/uniques/${item.slug()}?variant=${v.index()}"
				   hx-get="/poe/htmx/uniques/variant?slug=${item.slug()}&variant=${v.index()}"
				   hx-target="#uniqueVariantBox"
				   hx-swap="outerHTML"
				   hx-push-url="/poe/uniques/${item.slug()}?variant=${v.index()}">${PoeText.name(v.nameKo(), v.name())}</a>
			@endfor
		</div>
		@endif
	@endif
	@template.poe.htmx.uniqueDetail(item = item, base = base, variant = variant)
</div>
<%-- 변형을 바꾸면 "이 유니크로 최적화" 단추도 그 변형으로(C95) — htmx 응답일 때만(첫 화면엔 uniquePage 가 제자리에 그린다) --%>
@if(oob)
	@template.poe.htmx.optimizeUniqueLink(item = item, variant = variant, oob = true)
@endif

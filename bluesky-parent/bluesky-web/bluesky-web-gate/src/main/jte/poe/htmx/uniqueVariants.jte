@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.PoeText

@param PoeUniqueItem item
@param PoeBaseItem base = null
@param Integer variant = null

<%-- 변형 선택 + 아이템 레이어. 임프레션스(물리/화염/냉기/번개/카오스)처럼 **인게임에 동시에 존재하는**
     서로 다른 아이템들이라, 하나만 보여주면 나머지는 없는 셈이 된다. 칩으로 갈아끼운다.
     라벨은 게임에 없는 우리 쪽 장치라 툴팁 밖에 두고, 툴팁 자체는 인게임 그대로 유지한다. --%>
<div id="uniqueVariantBox" class="shrink-0 space-y-2">
	@if(item.variants() != null && !item.variants().isEmpty())
		!{ int current = variant != null ? variant.intValue() : (item.defaultVariant() != null ? item.defaultVariant().intValue() : item.variants().get(0).index()); }
		<div class="flex w-80 flex-wrap items-center gap-1">
			<span class="text-xs text-base-content/50">${PoeText.isKorean() ? "변형" : "Variant"}</span>
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
	@template.poe.htmx.uniqueDetail(item = item, base = base, variant = variant)
</div>

@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<PoeUniqueItem> items
@param int totalCount = 0
@param int matchedCount = 0
@param String listQ = ""
@param String listItemClass = "all"
@param int nextLimit = 0
@param java.util.Set<String> foulbornNames = java.util.Set.of()

!{
	String countLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.count"), items.size(), totalCount);
	String emptyLabel = MessageUtil.getMessage("poe.gems.empty");
	boolean capped = matchedCount > items.size();
	String moreLabel = capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.more"), Math.min(nextLimit, matchedCount) - items.size()) : null;
	String moreUrl = "/poe/htmx/uniques?limit=" + nextLimit
		+ "&q=" + java.net.URLEncoder.encode(listQ, java.nio.charset.StandardCharsets.UTF_8)
		+ "&itemClass=" + java.net.URLEncoder.encode(listItemClass, java.nio.charset.StandardCharsets.UTF_8);
	String cappedLabel = capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.capped"), matchedCount, items.size()) : null;
}

@if(totalCount == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-uniques.mjs")
@else
<div class="text-xs text-base-content/50 mb-2">${capped ? cappedLabel : countLabel}</div>
@if(items.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	<%-- poedb 식 목록 — 인게임 아이템 슬롯(어두운 프레임)에 큰 아이콘(72px). 일반 아이템 목록과 동일 스타일. --%>
	<div class="grid grid-cols-1 md:grid-cols-2 gap-3">
		@for(PoeUniqueItem item : items)
			!{
				String displayName = PoeText.name(item.nameKo(), item.name());
				String displayBase = PoeText.name(item.baseTypeKo(), item.baseType());
				List<String> displayImplicits = PoeText.lines(item.implicitsKo(), item.implicits());
				List<String> displayExplicits = PoeText.lines(item.explicitsKo(), item.explicits());
				String strLabel = MessageUtil.getMessage("poe.gems.attr.red");
				String dexLabel = MessageUtil.getMessage("poe.gems.attr.green");
				String intLabel = MessageUtil.getMessage("poe.gems.attr.blue");
				String levelLabel = MessageUtil.getMessage("poe.gems.detail.required.level");
			}
			<div class="poe-hover card bg-base-100 shadow-sm border border-base-200 cursor-pointer transition-colors duration-300 hover:border-orange-400/50 hover:duration-75"
			     hx-get="/poe/htmx/uniques/detail?slug=${item.slug()}"
			     hx-target="#poePreview"
			     hx-trigger="mouseenter delay:100ms"
			     hx-swap="innerHTML">
				<div class="card-body p-3">
				<div class="flex gap-3 items-start">
					@if(item.iconKey() != null)
						<%-- 인게임 아이템 슬롯 프레임 — 크기를 인벤토리 칸 비율(category 기준)로. 고유 전용 아이콘 우선, 없으면 베이스 아이콘 폴백. --%>
						!{ int[] ucells = net.luversof.web.gate.poe.PoeText.invCells(item.category()); int uunit = 58; }
						<div class="poe-item-slot shrink-0" style="width:${ucells[0] * uunit}px;height:${ucells[1] * uunit}px">
							<img src="/poe-assets/uniques/${item.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" onerror="this.onerror=null;this.src='/poe-assets/items/${item.iconKey()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}'" class="max-w-full max-h-full object-contain drop-shadow-[0_1px_3px_rgba(0,0,0,0.7)]" alt="" loading="lazy">
						</div>
					@endif
					<div class="min-w-0 flex-1 space-y-1">
					<%-- 제목 (고유색) --%>
					<div class="flex items-baseline gap-2">
						<%-- 이름 클릭 → 상세 페이지 --%>
						<a class="font-bold poe-name-unique truncate link-hover" href="/poe/uniques/${item.slug()}" title="${item.name()}">${displayName}</a>
						@if(PoeText.isKorean())
							<span class="text-xs text-base-content/40 truncate">${item.name()}</span>
						@endif
					</div>
					<%-- 베이스 + 요구조건(레벨·힘·민첩·지능, 0 은 생략) --%>
					<div class="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-base-content/55">
						<span class="truncate">${displayBase}</span>
						@if(item.requiredLevel() != null)
							<span class="text-base-content/40">${levelLabel} ${item.requiredLevel()}</span>
						@endif
						@if(item.reqStr() != null && item.reqStr() > 0)
							<span class="poe-gem-red">${strLabel} ${item.reqStr()}</span>
						@endif
						@if(item.reqDex() != null && item.reqDex() > 0)
							<span class="poe-gem-green">${dexLabel} ${item.reqDex()}</span>
						@endif
						@if(item.reqInt() != null && item.reqInt() > 0)
							<span class="poe-gem-blue">${intLabel} ${item.reqInt()}</span>
						@endif
						<%-- 리그 출처 — 444종이 특정 리그 기제에서만 나온다(하이스트 122, 브리치 34 …).
						     이게 없으면 "지금 파밍할 수 있는 것"과 "그 리그 콘텐츠를 돌아야 하는 것"이 구분되지 않는다. --%>
						@if(item.league() != null && !item.league().isBlank())
							<span class="badge badge-ghost badge-xs" title="${PoeText.isKorean() ? "이 고유는 해당 리그 콘텐츠에서 나옵니다" : "Drops from this league's content"}">${item.league()}</span>
						@endif
						<%-- 레거시 — 지금 리그에선 못 얻는 고유(레이스 보상 데미갓, 예언 전용 Fated, 삭제된 주얼).
						     게임의 고유 수집 탭이 챌린지 리그/스탠다드 어디에도 칸을 안 보여주는 것들이다.
						     시뮬레이터도 이걸 후보에서 뺀다 — 목록에서도 같은 사실을 알려야 화면끼리 말이 맞는다. --%>
						@if(item.legacy())
							<span class="badge badge-outline badge-xs border-warning/50 text-warning" title="${PoeText.isKorean() ? "현재 리그에서는 획득할 수 없습니다(스탠다드 보관분만 존재)" : "Cannot be obtained in the current league"}">${PoeText.isKorean() ? "레거시" : "Legacy"}</span>
						@endif
						<%-- 변형 — 같은 이름·같은 베이스인데 모드 구성이 다른 것들(임프레션스 5원소 등).
						     목록엔 기본 변형만 나오므로, 나머지가 있다는 사실은 여기서 알린다. --%>
						@if(item.variants() != null && item.variants().size() > 1)
							<span class="badge badge-outline badge-xs border-fuchsia-500/40 text-fuchsia-500" title="${PoeText.isKorean() ? "모드 구성이 다른 변형이 있습니다 — 상세에서 선택" : "Has alternate variants - pick one on the detail page"}">${PoeText.isKorean() ? "변형 " + item.variants().size() : item.variants().size() + " variants"}</span>
						@endif
						<%-- 인게임 "삿된 XXX" 가 존재하는 아이템 — 상세에서 붙을 수 있는 옵션 풀을 볼 수 있다 --%>
						@if(foulbornNames != null && foulbornNames.contains(item.name()))
							<a class="badge badge-outline badge-xs border-fuchsia-500/40 text-fuchsia-500" href="/poe/uniques/${item.slug()}" title="${PoeText.isKorean() ? "삿된 오브로 바꿀 수 있는 옵션이 있습니다" : "Has Foulborn options"}">${PoeText.isKorean() ? "삿된" : "Foulborn"}</a>
						@endif
					</div>
					<%-- 옵션 — 인게임 툴팁 구조: 고정(암시)은 상단, 구분선 아래 접두/접미(폭발).
					     모드 텍스트는 인게임 모드 파랑, 변동 수치는 highlightValues 로 별도 색(.poe-val) 강조. --%>
					@if(!displayImplicits.isEmpty() || !displayExplicits.isEmpty())
						<div class="mt-1 space-y-0.5">
							@if(!displayImplicits.isEmpty())
								@for(String imp : displayImplicits)
									<div class="text-[11px] leading-5 text-[#8888ff]">$unsafe{PoeText.highlightValues(imp)}</div>
								@endfor
								@if(!displayExplicits.isEmpty())
									<div class="my-1 border-t border-base-content/15"></div>
								@endif
							@endif
							@for(String ex : displayExplicits)
								<div class="text-[11px] leading-5 text-[#8888ff]">$unsafe{PoeText.highlightValues(ex)}</div>
							@endfor
						</div>
					@endif
					</div>
				</div>
				</div>
			</div>
		@endfor
	</div>
@endif

	@if(capped)
		<%-- 더 보기 — 상한(90)만 두고 더 볼 방법이 없으면 1,268개 중 나머지는 검색어를 정확히 아는
		     사람만 도달할 수 있었다. 한 단계(90개)씩 늘려 같은 목록 자리에 다시 그린다. --%>
		<div class="flex justify-center pt-3">
			<button type="button" class="btn btn-sm btn-outline"
			        hx-get="${moreUrl}"
			        hx-target="#poeUniqueList"
			        hx-swap="innerHTML"
			        hx-disabled-elt="this">${moreLabel}</button>
		</div>
	@endif
@endif

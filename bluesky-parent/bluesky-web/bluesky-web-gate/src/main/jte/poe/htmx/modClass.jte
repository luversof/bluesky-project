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

	<div class="grid gap-4 md:grid-cols-2">
		<%-- 접두 --%>
		<div>
			<h2 class="mb-2 flex items-baseline gap-2 font-bold text-[#8888ff]">
				${ko ? "접두" : "Prefixes"}
				<span class="text-xs text-base-content/40">${mods.prefixes().size()}</span>
			</h2>
			<div class="space-y-2">
				@for(PoeModClass.NamedFamily nf : mods.prefixes())
					@template.poe.htmx.modFamilyCard(key = nf.key(), family = nf.family(), ko = ko)
				@endfor
			</div>
		</div>
		<%-- 접미 --%>
		<div>
			<h2 class="mb-2 flex items-baseline gap-2 font-bold text-[#8888ff]">
				${ko ? "접미" : "Suffixes"}
				<span class="text-xs text-base-content/40">${mods.suffixes().size()}</span>
			</h2>
			<div class="space-y-2">
				@for(PoeModClass.NamedFamily nf : mods.suffixes())
					@template.poe.htmx.modFamilyCard(key = nf.key(), family = nf.family(), ko = ko)
				@endfor
			</div>
		</div>
	</div>

	<%-- 엘드리치 임플리싯(총주교/포식자) — 대상 슬롯일 때만. 제작 접두/접미와 별개(화폐로 붙이는 임플리싯) --%>
	@if(eldritch != null)
		<div class="mt-4 rounded-2xl bg-base-100 p-4 shadow-sm">
			<div class="mb-2 flex flex-wrap items-baseline gap-2">
				<h2 class="font-bold text-amber-500">${ko ? "엘드리치 임플리싯" : "Eldritch implicits"}</h2>
				<span class="text-sm text-base-content/50">${ko ? "총주교·포식자 화폐로 부여 — 팩션당 1개씩 최대 2개" : "Exarch + Eater — up to one each"}</span>
			</div>
			@template.poe.htmx.eldritchPools(eldritch = eldritch, ko = ko)
		</div>
	@endif
@endif

@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param String title
@param Poe2.Meta meta = null
@param String backHref = null
@param String backLabel = null
@param String subtitle = null
@param gg.jte.Content actions = null

<%-- PoE2 화면 공용 머리 — 제목 + 패치 배지(PoE1 과 구분되게 "PoE2 패치"). 데이터가 없으면 추출 안내.
     상세 화면은 PoE1 상세와 같은 한 줄: ← 목록 · 제목 · 영문명(한국어일 때) · 패치 · 오른쪽 단추(10-12 C275) --%>
<div class="flex flex-wrap items-baseline gap-3">
	@if(backHref != null)
		<a href="${backHref}" class="link link-hover text-sm text-base-content/60">← ${backLabel}</a>
	@endif
	<h1 class="text-2xl font-bold">${title}</h1>
	@if(subtitle != null && !subtitle.isBlank())
		<span class="text-base-content/60 text-sm" data-poe-orig>${subtitle}</span>
	@endif
	@if(meta != null && meta.patch() != null && !meta.patch().isBlank())
		<span class="badge badge-ghost">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.patch"), meta.patch())}</span>
	@endif
	@if(actions != null)
		<div class="ml-auto flex flex-wrap items-center gap-2">${actions}</div>
	@endif
</div>
@if(meta == null || meta.patch() == null || meta.patch().isBlank())
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"), actionLabel = MessageUtil.getMessage("layout.menu.poe2.admin"), actionHref = "/poe2/admin")
@endif

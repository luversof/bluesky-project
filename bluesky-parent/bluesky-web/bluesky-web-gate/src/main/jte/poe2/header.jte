@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param String title
@param Poe2.Meta meta = null

<%-- PoE2 화면 공용 머리 — 제목 + 패치 배지(PoE1 과 구분되게 "PoE2 패치"). 데이터가 없으면 추출 안내. --%>
<div class="flex flex-wrap items-baseline gap-3">
	<h1 class="text-2xl font-bold">${title}</h1>
	@if(meta != null && meta.patch() != null && !meta.patch().isBlank())
		<span class="badge badge-ghost">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.patch"), meta.patch())}</span>
	@endif
</div>
@if(meta == null || meta.patch() == null || meta.patch().isBlank())
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"), actionLabel = MessageUtil.getMessage("layout.menu.poe2.admin"), actionHref = "/poe2/admin")
@endif

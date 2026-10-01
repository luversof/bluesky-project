@import io.github.luversof.boot.context.support.MessageUtil

@param String active = "uniques"

<%-- 아이템 메뉴의 두 탭(고유 / 일반) — PoE1 과 같은 구성. data-poe-item-tab: common.ts 가 활성 칩(data-slot) + 검색어를 ?slot&q 로
     넘겨 탭을 바꿔도 필터가 유지된다(PoE1 poe/itemTabs.jte 와 같은 흐름, 10-01) --%>
<div role="tablist" class="tabs tabs-box tabs-sm w-fit">
	<a role="tab" href="/poe2/uniques" data-poe-item-tab class="tab ${"uniques".equals(active) ? "tab-active" : ""}" aria-selected="${"uniques".equals(active)}">${MessageUtil.getMessage("poe2.items.tab.uniques")}</a>
	<a role="tab" href="/poe2/items" data-poe-item-tab class="tab ${"items".equals(active) ? "tab-active" : ""}" aria-selected="${"items".equals(active)}">${MessageUtil.getMessage("poe2.items.tab.bases")}</a>
</div>

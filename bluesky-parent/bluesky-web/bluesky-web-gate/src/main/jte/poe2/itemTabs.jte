@import io.github.luversof.boot.context.support.MessageUtil

@param String active = "uniques"

<%-- 아이템 메뉴의 두 탭(고유 / 일반) — PoE1 과 같은 구성. data-poe-item-tab: common.ts 가 활성 칩(data-slot) + 검색어를 ?slot&q 로
     넘겨 탭을 바꿔도 필터가 유지된다(PoE1 poe/itemTabs.jte 와 같은 흐름, 10-01) --%>
<%-- 모양도 PoE1 탭과 같게(10-01): 폭 전체 큰 탭, 고른 탭은 주 색으로 채운다. 글자도 PoE1 키(poe.tab.uniques · poe.tab.items — 10-02 문구 통일) --%>
!{
	String activeCls = "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm";
	String inactiveCls = "tab tab-lg flex-1 text-base-content/60 hover:text-base-content";
}
<div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1">
	<a role="tab" href="/poe2/uniques" data-poe-item-tab class="${"uniques".equals(active) ? activeCls : inactiveCls}" aria-selected="${"uniques".equals(active)}">${MessageUtil.getMessage("poe.tab.uniques")}</a>
	<a role="tab" href="/poe2/items" data-poe-item-tab class="${"items".equals(active) ? activeCls : inactiveCls}" aria-selected="${"items".equals(active)}">${MessageUtil.getMessage("poe.tab.items")}</a>
</div>

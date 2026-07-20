@import io.github.luversof.boot.context.support.MessageUtil

@param String activeTab = "uniques"

!{
	String uniquesTabLabel = MessageUtil.getMessage("poe.tab.uniques");
	String baseItemsTabLabel = MessageUtil.getMessage("poe.tab.items");
	String activeClass = "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm";
	String inactiveClass = "tab tab-lg flex-1 text-base-content/60 hover:text-base-content";
}

<%-- 아이템 세부 메뉴: 고유 | 일반 (좌측 서브메뉴 '아이템' 하위의 상단 탭)
     data-poe-item-tab: common.ts 가 클릭 시 현재 활성 칩 slot + 검색어를 ?slot&q 로 넘겨 탭 전환 후에도 필터 유지 --%>
<div class="tabs tabs-box w-full bg-base-200/70 p-1">
	<a href="/poe/uniques" data-poe-item-tab class="${"uniques".equals(activeTab) ? activeClass : inactiveClass}">${uniquesTabLabel}</a>
	<a href="/poe/items" data-poe-item-tab class="${"items".equals(activeTab) ? activeClass : inactiveClass}">${baseItemsTabLabel}</a>
</div>

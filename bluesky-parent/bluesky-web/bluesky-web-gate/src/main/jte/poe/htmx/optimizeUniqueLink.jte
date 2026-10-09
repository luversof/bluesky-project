@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.PoeText

@param PoeUniqueItem item
@param Integer variant = null
@param boolean oob = false

<%-- "이 유니크로 최적화" 단추(10-04 C95) — 고른 변형까지 시뮬로 넘긴다(uniques=slug:v번호). 변형 칩을 누르면 htmx 가 이 단추를 oob 로 함께 바꾼다.
     시뮬 강제 후보는 장비 칸에 끼는 고유만이라(주얼 · 팅크처 · 낚싯대 제외 — PoeViewController.sim) 그 밖엔 단추를 안 보인다
     (예전엔 주얼 상세에도 보여, 누르면 시뮬이 조용히 무시했다. PoE2 고유 상세는 처음부터 장착 가능한 것만 보였다). --%>
!{
	boolean forceable = item.category() == null || !java.util.Set.of("jewel", "tincture", "fishing").contains(item.category());
	boolean withVariant = variant != null && item.variants() != null && item.variants().size() > 1;
}
@if(forceable)
	<a id="poeOptimizeUnique" hx-swap-oob="${oob ? "true" : null}" class="btn btn-sm btn-primary btn-outline ml-auto" data-optimize-unique
	   href="/poe/sim?uniques=${item.slug()}${withVariant ? ":v" + variant : ""}">${PoeText.isKorean() ? "이 유니크로 최적화 →" : "Optimize with this unique →"}</a>
@endif

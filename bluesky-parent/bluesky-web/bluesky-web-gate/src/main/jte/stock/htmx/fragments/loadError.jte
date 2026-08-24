@import io.github.luversof.boot.context.support.MessageUtil

<%-- 백엔드 호출이 실패했을 때 htmx 가 갈아끼울 조각.
     이게 없으면 공통 예외 처리기가 본문 없는 200 을 돌려줘 화면이 아무 안내 없이 빈다. --%>
<div class="alert alert-error" role="alert">
    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
    </svg>
    <div class="min-w-0">
        <div class="font-semibold">${MessageUtil.getMessage("stock.error.fragment.title")}</div>
        <div class="text-sm opacity-80 whitespace-normal">${MessageUtil.getMessage("stock.error.fragment.desc")}</div>
    </div>
</div>

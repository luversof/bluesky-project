<%-- 화면 안내 상자: 이 관리 화면이 무엇을 정하는지 짧게(접을 수 있음). 항목별 자세한 설명은 ⓘ 도움말(_components/help) 로.
     처음엔 접혀 있고(익숙해지면 매번 접을 필요 없게 - 사용자 결정 11-26), rememberKey 가 있으면 접고 편 상태를 이 브라우저에 기억한다
     (js/ui.js data-remember-open, 접히는 카드와 같은 동작) --%>
@param String title
@param gg.jte.Content content
@param String cssClass = "mb-4"
@param boolean open = false
@param String rememberKey = null

<details class="guide ${cssClass}" open="${open}" data-remember-open="${rememberKey}">
	<summary class="guide-title">${title}</summary>
	<div class="guide-body">${content}</div>
</details>

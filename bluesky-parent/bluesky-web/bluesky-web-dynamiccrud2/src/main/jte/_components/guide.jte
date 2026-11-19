<%-- 화면 안내 상자: 이 관리 화면이 무엇을 정하는지 짧게(접을 수 있음). 항목별 자세한 설명은 ⓘ 도움말(_components/help) 로 --%>
@param String title
@param gg.jte.Content content
@param String cssClass = "mb-4"

<details class="guide ${cssClass}" open>
	<summary class="guide-title">${title}</summary>
	<div class="guide-body">${content}</div>
</details>

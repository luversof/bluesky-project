"use strict";
/**
 * 주식 손익 통계 화면 스크립트
 */
// DOM 로드 완료 후 초기화
document.addEventListener("DOMContentLoaded", () => {
    initializeTradeProfitForm();
});
/**
 * 손익 조회 폼 초기화
 */
function initializeTradeProfitForm() {
    const form = document.getElementById("tradeProfitForm");
    if (!form)
        return;
    // HTMX 이벤트 리스너
    setupHtmxEventListeners();
}
/**
 * Date를 datetime-local input 형식으로 변환
 */
function formatDateTimeLocal(date) {
    const year = date.getFullYear();
    const month = padZero(date.getMonth() + 1);
    const day = padZero(date.getDate());
    const hours = padZero(date.getHours());
    const minutes = padZero(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}
/**
 * 숫자를 2자리 문자열로 변환
 */
function padZero(num) {
    return num < 10 ? "0" + num : String(num);
}
/**
 * HTMX 이벤트 리스너 설정
 */
function setupHtmxEventListeners() {
    // HTMX 요청 전 처리
    document.body.addEventListener("htmx:configRequest", (event) => {
        const detail = event.detail;
        // accountIdList: 쉼표로 구분된 문자열 또는 배열 둘 다 처리
        if (detail.parameters.accountIdList) {
            let accountIds = detail.parameters.accountIdList;
            if (Array.isArray(accountIds)) {
                // HTMX may provide repeated params as an array already
                accountIds = accountIds
                    .map((id) => String(id).trim())
                    .filter((id) => id.length > 0);
            }
            else {
                accountIds = String(accountIds)
                    .split(",")
                    .map((id) => id.trim())
                    .filter((id) => id.length > 0);
            }
            if (accountIds.length > 0) {
                // 배열을 다시 서버가 이해할 수 있는 형식으로 변환
                delete detail.parameters.accountIdList;
                for (let index = 0; index < accountIds.length; index++) {
                    detail.parameters[`accountIdList[${index}]`] = accountIds[index];
                }
            }
            else {
                delete detail.parameters.accountIdList;
            }
        }
        // stockItemIdList: 쉼표 문자열 또는 배열 처리
        if (detail.parameters.stockItemIdList) {
            let stockItemIds = detail.parameters.stockItemIdList;
            if (Array.isArray(stockItemIds)) {
                stockItemIds = stockItemIds
                    .map((id) => String(id).trim())
                    .filter((id) => id.length > 0);
            }
            else {
                stockItemIds = String(stockItemIds)
                    .split(",")
                    .map((id) => id.trim())
                    .filter((id) => id.length > 0);
            }
            if (stockItemIds.length > 0) {
                delete detail.parameters.stockItemIdList;
                for (let index = 0; index < stockItemIds.length; index++) {
                    detail.parameters[`stockItemIdList[${index}]`] = stockItemIds[index];
                }
            }
            else {
                delete detail.parameters.stockItemIdList;
            }
        }
        // datetime-local을 ISO-8601 형식으로 변환
        if (detail.parameters.startDate) {
            detail.parameters.startDate = convertToIsoWithOffset(detail.parameters.startDate);
        }
        if (detail.parameters.endDate) {
            detail.parameters.endDate = convertToIsoWithOffset(detail.parameters.endDate);
        }
    });
    // HTMX 요청 성공 후 처리
    document.body.addEventListener("htmx:afterSwap", (event) => {
        console.log("Trade profit data loaded successfully");
    });
    // HTMX 요청 실패 시 처리
    document.body.addEventListener("htmx:responseError", (event) => {
        console.error("Failed to load trade profit data", event.detail);
        showError("데이터 조회 중 오류가 발생했습니다.");
    });
}
/**
 * datetime-local 값을 ISO-8601 형식 (with offset)으로 변환
 */
function convertToIsoWithOffset(dateTimeLocal) {
    if (!dateTimeLocal)
        return "";
    const date = new Date(dateTimeLocal);
    // 한국 시간대 오프셋 (+09:00)
    const offset = "+09:00";
    // ISO 문자열 생성
    const year = date.getFullYear();
    const month = padZero(date.getMonth() + 1);
    const day = padZero(date.getDate());
    const hours = padZero(date.getHours());
    const minutes = padZero(date.getMinutes());
    const seconds = padZero(date.getSeconds());
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}${offset}`;
}
/**
 * 에러 메시지 표시
 */
function showError(message) {
    const resultDiv = document.getElementById("tradeProfitResult");
    if (!resultDiv)
        return;
    resultDiv.innerHTML = `
		<div class="alert alert-error">
			<svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
			</svg>
			<span>${message}</span>
		</div>
	`;
}
// 전역 스코프에 노출 (필요한 경우)
window.tradeProfitModule = {
    formatDateTimeLocal,
    convertToIsoWithOffset,
};

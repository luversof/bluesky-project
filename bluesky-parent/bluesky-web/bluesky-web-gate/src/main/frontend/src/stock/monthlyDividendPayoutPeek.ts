export {};

// 관리 > 월배당 기준: 프로필 행 안에서 그 종목의 지급 이력을 펼친다(사용자 요청 2026-09-29, A안).
//
// 예전에는 "이 종목 보기" 가 페이지를 다시 불러 맨 위로 돌아갔고, 지급 이력은 프로필 목록 전체 아래에 있어
// 프로필 21 개일 때 1440px 에서 2,019px · 휴대폰에서 2,542px 을 내려가야 했다. 여기서는 누른 행 바로 아래에
// 줄 하나를 만들어 서버 조각(/stock/dividend/monthly-reference/payout/peek)을 싣는다 - 페이지를 다시 부르지 않는다.
//
// 펼친 줄은 열 때 만들고 닫을 때 지운다. 끌어서 순서 바꾸기(monthlyDividendProfileOrder)는 "바로 다음 줄" 을 기준으로
// 행을 옮기므로 사이에 끼인 줄이 있으면 엉뚱한 자리에 떨어진다 - 끌기를 시작하면 펼친 줄을 모두 닫는다.

const TOGGLE = "button[data-payout-peek-toggle]";
const PEEK_ROW = "tr[data-payout-peek-row]";

function setExpanded(button: HTMLElement, expanded: boolean): void {
	button.setAttribute("aria-expanded", expanded ? "true" : "false");
	const icon = button.querySelector<HTMLElement>("[data-payout-peek-icon]");
	if (icon) {
		icon.textContent = expanded ? "▴" : "▾";
	}
}

function closeAll(): void {
	document.querySelectorAll(PEEK_ROW).forEach((row) => row.remove());
	document
		.querySelectorAll<HTMLElement>(TOGGLE + '[aria-expanded="true"]')
		.forEach((button) => setExpanded(button, false));
}

function open(button: HTMLButtonElement, row: HTMLTableRowElement): void {
	const detail = document.createElement("tr");
	detail.dataset.payoutPeekRow = row.dataset.symbol || "";
	const controls = button.getAttribute("aria-controls");
	if (controls) {
		detail.id = controls;
	}
	const cell = document.createElement("td");
	cell.colSpan = row.cells.length;
	cell.setAttribute("aria-live", "polite");
	cell.textContent = button.dataset.peekLoading || "";
	detail.appendChild(cell);
	row.after(detail);
	setExpanded(button, true);

	const url = button.dataset.peekUrl || "";
	const failed = (): void => {
		if (detail.isConnected) {
			cell.textContent = button.dataset.peekFailed || "";
		}
	};
	fetch(url, { credentials: "same-origin", headers: { "HX-Request": "true" } })
		.then((response) => {
			// 세션이 끊기면 로그인 화면으로 넘어간다 - 그 화면을 줄 안에 싣지 않는다.
			if (!response.ok || response.redirected) {
				throw new Error(String(response.status));
			}
			return response.text();
		})
		.then((html) => {
			if (!detail.isConnected) {
				return;
			}
			const doc = new DOMParser().parseFromString(html, "text/html");
			cell.replaceChildren(...Array.from(doc.body.childNodes));
		})
		.catch(failed);
}

document.addEventListener("click", (event) => {
	const target = event.target as Element | null;
	const button = target ? target.closest<HTMLButtonElement>(TOGGLE) : null;
	if (!button) {
		return;
	}
	const row = button.closest<HTMLTableRowElement>("tr");
	if (!row) {
		return;
	}
	if (button.getAttribute("aria-expanded") === "true") {
		const next = row.nextElementSibling;
		if (next && next.matches(PEEK_ROW)) {
			next.remove();
		}
		setExpanded(button, false);
		return;
	}
	open(button, row);
});

// 캡처 단계에서 먼저 닫는다 - 순서 바꾸기 스크립트가 행 위치를 읽기 전에.
document.addEventListener("dragstart", closeAll, true);

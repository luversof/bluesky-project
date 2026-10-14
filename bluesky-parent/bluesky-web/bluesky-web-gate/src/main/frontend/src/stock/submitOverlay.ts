export {};

// 관리 화면 '월배당 기준' 의 가져오기 폼들은 평범한 POST 폼이라 htmx 의 hx-indicator 가 붙지 않는다.
// 그런데 출처 일괄 가져오기는 프로필마다 외부 ETF 사이트를 <b>순서대로</b> 내려받는다
// (실측 2026-09-08: riseetf.co.kr 페이지 하나가 8.65 초, 월 중 대상 프로필이 4 개).
// 그동안 화면은 아무 말도 하지 않아 멈춘 것처럼 보이고, 다시 누르면 같은 가져오기가 한 번 더 돈다.
//
// 그래서 (1) 오버레이로 진행 중임을 알리고 (2) 두 번째 제출을 막는다.

interface OverlayText {
	title: string;
	desc: string;
}

interface OverlayDataset {
	submitOverlayTitle?: string;
	submitOverlayDesc?: string;
}

/** 폼이 스스로 밝힌 문구를 쓰고, 없으면 마크업에 박아 둔 기본 문구로 돌아간다. */
function overlayTextFor(
	dataset: OverlayDataset,
	fallback: OverlayText,
): OverlayText {
	const title = (dataset.submitOverlayTitle ?? "").trim();
	const desc = (dataset.submitOverlayDesc ?? "").trim();
	return {
		title: title.length > 0 ? title : fallback.title,
		desc: desc.length > 0 ? desc : fallback.desc,
	};
}

// 가져오기는 외부에서 받아 저장까지 하므로 중복 제출은 같은 일을 한 번 더 시키고
// 결과 메시지만 덮어쓴다. 화면 전체에 하나의 자물쇠를 둔다 - 다른 폼을 눌러도 마찬가지다.
function createSubmitGuard() {
	let submitting = false;
	return {
		isSubmitting: () => submitting,
		/** 처음 제출이면 true, 이미 보내는 중이면 false(=막아야 한다). */
		begin() {
			if (submitting) {
				return false;
			}
			submitting = true;
			return true;
		},
		reset() {
			submitting = false;
		},
	};
}

// 브라우저에서는 쓰이지 않는다. 검증용으로만 읽는다(tableSort 와 같은 방식).
(globalThis as any).__submitOverlayInternals = {
	overlayTextFor,
	createSubmitGuard,
};

(() => {
	const overlay = document.querySelector<HTMLElement>(
		"[data-submit-overlay-panel]",
	);
	// 폼은 그때그때 찾는다 - 관리 월배당 목록은 구역만 바꿔 끼워(2026-10-13) 행의 "출처에서 가져오기" 폼(2026-10-14)이 나중에 들어온다.
	const currentForms = () =>
		Array.from(
			document.querySelectorAll<HTMLFormElement>("form[data-submit-overlay]"),
		);
	if (overlay === null) {
		return;
	}

	const titleElement = overlay.querySelector<HTMLElement>(
		"[data-submit-overlay-heading]",
	);
	const descElement = overlay.querySelector<HTMLElement>(
		"[data-submit-overlay-note]",
	);
	const fallback: OverlayText = {
		title: titleElement?.textContent ?? "",
		desc: descElement?.textContent ?? "",
	};
	const guard = createSubmitGuard();
	const lockedButtons: HTMLButtonElement[] = [];

	// 문서에서 한 번 듣는다(위임). 확인 창(data-confirm)이 취소했으면 덮개를 띄우지 않는다.
	document.addEventListener("submit", (event) => {
		// 지금 화면의 오버레이 폼인가 - 목록에서 찾는다(instanceof 는 시험 가짜에서 쓸 수 없다).
		const form = currentForms().find((one) => one === event.target);
		if (form === undefined) {
			return;
		}
		if (event.defaultPrevented) {
			return;
		}
		if (!guard.begin()) {
			event.preventDefault();
			return;
		}
		show(form);
	});

	function show(form: HTMLFormElement) {
		const text = overlayTextFor(form.dataset as OverlayDataset, fallback);
		if (titleElement !== null) {
			titleElement.textContent = text.title;
		}
		if (descElement !== null) {
			descElement.textContent = text.desc;
		}
		overlay?.classList.remove("hidden");

		// 버튼 잠금은 다음 틱으로 미룬다. submit 처리 도중에 disabled 로 만들면
		// 이름을 가진 제출 버튼의 값이 폼 데이터에서 빠질 수 있다.
		window.setTimeout(() => {
			for (const target of currentForms()) {
				const buttons = target.querySelectorAll<HTMLButtonElement>(
					'button[type="submit"]',
				);
				for (const button of Array.from(buttons)) {
					button.disabled = true;
					lockedButtons.push(button);
				}
			}
		}, 0);
	}

	function reset() {
		overlay?.classList.add("hidden");
		for (const button of lockedButtons.splice(0)) {
			button.disabled = false;
		}
		guard.reset();
	}

	// htmx 로 보내는 폼(관리 월배당 목록 행의 "출처에서 가져오기", 2026-10-14)은 htmx 가 제출을 가로채 위 처리기가 덮개를 띄우지 않는다 -
	// 요청이 시작될 때 띄우고 끝나면 걷는다(페이지가 바뀌지 않으므로 걷어야 한다).
	document.addEventListener("htmx:beforeRequest", (event) => {
		const elt = (event as CustomEvent).detail?.elt;
		const form = currentForms().find((one) => one === elt);
		if (form === undefined) {
			return;
		}
		if (!guard.begin()) {
			event.preventDefault();
			return;
		}
		htmxOverlayActive = true;
		show(form);
	});
	// 끝은 보낸 폼에서 알리는데, 구역을 바꿔 끼우면 그 폼이 문서에서 빠져 알림이 문서까지 오지 않는다(실측 2026-10-14: 덮개가 남았다).
	// 그래서 덮개를 띄운 htmx 요청을 기억해 두고, 문서까지 오는 끝 알림(정착 · 응답 오류 · 전송 오류)에서 걷는다.
	let htmxOverlayActive = false;
	const endHtmxOverlay = () => {
		if (htmxOverlayActive) {
			htmxOverlayActive = false;
			reset();
		}
	};
	for (const type of ["htmx:afterSettle", "htmx:responseError", "htmx:sendError", "htmx:timeout"]) {
		document.addEventListener(type, endHtmxOverlay);
	}

	// 뒤로 가기로 bfcache 에서 돌아오면 DOM 이 그대로 살아 돌아온다.
	// 풀어 주지 않으면 오버레이가 화면을 덮은 채로 멈춘다.
	window.addEventListener("pageshow", (event) => {
		if ((event as PageTransitionEvent).persisted) {
			reset();
		}
	});
})();

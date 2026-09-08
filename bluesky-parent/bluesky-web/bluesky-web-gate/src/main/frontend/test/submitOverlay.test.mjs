// 관리 > 월배당 기준의 가져오기 폼에 붙는 로딩 오버레이.
//
// 왜 필요한가(실측 2026-09-08): 출처 일괄 가져오기는 대상 프로필마다 외부 ETF 사이트를 차례로
// 내려받는다. 한 건(riseetf.co.kr)이 8.65 초 걸렸고 월 중 대상 프로필은 4 개다. 그동안 화면은
// 아무 표시도 하지 않아 멈춘 것처럼 보였고, 다시 누르면 같은 가져오기가 한 번 더 돌았다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

function makeClassList(initial) {
	const set = new Set(initial);
	return {
		add: (name) => set.add(name),
		remove: (name) => set.delete(name),
		has: (name) => set.has(name),
	};
}

function makeButton() {
	return { disabled: false, type: "submit" };
}

function makeForm(dataset) {
	const buttons = [makeButton()];
	const listeners = [];
	return {
		dataset,
		buttons,
		listeners,
		addEventListener: (type, fn) => listeners.push([type, fn]),
		querySelectorAll: (selector) =>
			selector === 'button[type="submit"]' ? buttons : [],
		submit() {
			let prevented = false;
			const event = {
				preventDefault: () => {
					prevented = true;
				},
			};
			for (const [type, fn] of listeners) {
				if (type === "submit") {
					fn(event);
				}
			}
			return prevented;
		},
	};
}

const overlayTitle = { textContent: "가져오는 중입니다..." };
const overlayDesc = { textContent: "잠시만 기다려주세요." };
const overlay = {
	classList: makeClassList(["hidden"]),
	querySelector: (selector) =>
		selector === "[data-submit-overlay-heading]"
			? overlayTitle
			: selector === "[data-submit-overlay-note]"
				? overlayDesc
				: null,
};

const bulkForm = makeForm({
	submitOverlayTitle: "월 중 배당 일괄 가져오기",
	submitOverlayDesc: "출처 사이트에서 내려받는 중이라 시간이 걸릴 수 있습니다.",
});
const pasteForm = makeForm({ submitOverlayTitle: "", submitOverlayDesc: "" });
const forms = [bulkForm, pasteForm];

const pageshowListeners = [];
const pendingTimeouts = [];

globalThis.document = {
	readyState: "complete",
	addEventListener: () => {},
	querySelector: (selector) =>
		selector === "[data-submit-overlay-panel]" ? overlay : null,
	querySelectorAll: (selector) =>
		selector === "form[data-submit-overlay]" ? forms : [],
};
globalThis.window = {
	addEventListener: (type, fn) => {
		if (type === "pageshow") {
			pageshowListeners.push(fn);
		}
	},
	setTimeout: (fn) => {
		pendingTimeouts.push(fn);
		return pendingTimeouts.length;
	},
};

/** 미뤄 둔 setTimeout 콜백을 흘려보낸다(버튼 잠금은 다음 틱에 일어난다). */
function flushTimeouts() {
	while (pendingTimeouts.length > 0) {
		pendingTimeouts.shift()();
	}
}

function firePageshow(persisted) {
	for (const fn of pageshowListeners) {
		fn({ persisted });
	}
}

await import("../../resources/static/js/stock/submitOverlay.js");
const internals = globalThis.__submitOverlayInternals;

test("내부 규칙이 노출돼 있다", () => {
	assert.ok(internals, "submitOverlay 가 __submitOverlayInternals 를 노출하지 않는다");
	assert.equal(typeof internals.overlayTextFor, "function");
	assert.equal(typeof internals.createSubmitGuard, "function");
});

test("폼이 밝힌 문구를 쓰고, 비어 있으면 기본 문구로 돌아간다", () => {
	const fallback = { title: "가져오는 중입니다...", desc: "잠시만 기다려주세요." };

	assert.deepEqual(
		internals.overlayTextFor(
			{ submitOverlayTitle: "월말 배당 일괄 가져오기", submitOverlayDesc: "느립니다" },
			fallback,
		),
		{ title: "월말 배당 일괄 가져오기", desc: "느립니다" },
	);
	// 공백만 있는 값도 없는 것으로 본다 - 빈 제목이 뜨면 무엇이 도는지 알 수 없다.
	assert.deepEqual(
		internals.overlayTextFor({ submitOverlayTitle: "  ", submitOverlayDesc: "" }, fallback),
		fallback,
	);
	assert.deepEqual(internals.overlayTextFor({}, fallback), fallback);
});

test("자물쇠는 처음 한 번만 열린다", () => {
	const guard = internals.createSubmitGuard();

	assert.equal(guard.isSubmitting(), false);
	assert.equal(guard.begin(), true);
	assert.equal(guard.begin(), false, "두 번째 제출은 막혀야 한다");
	guard.reset();
	assert.equal(guard.begin(), true, "되돌아온 뒤에는 다시 보낼 수 있어야 한다");
});

test("제출하면 그 폼의 문구로 오버레이가 뜬다", () => {
	assert.equal(overlay.classList.has("hidden"), true, "처음에는 숨어 있다");

	const prevented = bulkForm.submit();

	assert.equal(prevented, false, "첫 제출은 그대로 보내야 한다");
	assert.equal(overlay.classList.has("hidden"), false);
	assert.equal(overlayTitle.textContent, "월 중 배당 일괄 가져오기");
	assert.equal(
		overlayDesc.textContent,
		"출처 사이트에서 내려받는 중이라 시간이 걸릴 수 있습니다.",
	);
});

test("버튼 잠금은 다음 틱이다 - 제출 값이 폼에서 빠지면 안 된다", () => {
	assert.equal(
		bulkForm.buttons[0].disabled,
		false,
		"submit 처리 중에 disabled 로 만들면 이름 있는 제출 버튼의 값이 빠진다",
	);

	flushTimeouts();

	assert.equal(bulkForm.buttons[0].disabled, true);
	assert.equal(pasteForm.buttons[0].disabled, true, "다른 가져오기 버튼도 함께 잠근다");
});

test("보내는 중에 다른 폼을 눌러도 막는다", () => {
	const prevented = pasteForm.submit();

	assert.equal(prevented, true);
	assert.equal(
		overlayTitle.textContent,
		"월 중 배당 일괄 가져오기",
		"막힌 제출이 문구를 바꾸면 안 된다",
	);
});

test("뒤로 가기로 돌아오면 오버레이를 걷는다", () => {
	firePageshow(false);
	assert.equal(overlay.classList.has("hidden"), false, "새로 그린 화면은 건드리지 않는다");

	firePageshow(true);

	assert.equal(overlay.classList.has("hidden"), true);
	assert.equal(bulkForm.buttons[0].disabled, false);
	assert.equal(pasteForm.buttons[0].disabled, false);
	assert.equal(pasteForm.submit(), false, "다시 보낼 수 있어야 한다");
});

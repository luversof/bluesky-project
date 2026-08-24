// 게이트가 만드는 JSON 오류 본문과 클라이언트의 오류 처리가 짝이 맞는지 본다.
//
// 월배당 참조 화면의 "표시 순서 저장"(PUT /stock/dividend/monthly-reference/profile/order)은 실패를
// 세 가지로 돌려준다 - 401 로그인 필요 / 400 검증 실패 / 500 서버 오류. 셋 다 본문이
// {"message": ..., "isDisplayableMessage": true} 형태다.
//
// 클라이언트(handleApiError)는 body.isDisplayableMessage 와 body.message 를 <b>최상위에서</b> 읽는다.
// 어느 한쪽만 바뀌면 사용자는 서버가 보낸 사유("로그인이 필요합니다") 대신 화면 기본 문구
// ("표시 순서를 저장하지 못했습니다")만 보게 된다 - 세션이 끊겼다는 사실이 사라진다.
//
// 참고: bluesky-boot 의 표준 오류 본문은 {status, title, result:{message, displayableMessage}} 로
// 키 이름도 위치도 다르다. 그래서 이 컨트롤러가 최상위 형태를 직접 만드는 것이 계약의 핵심이다.
import assert from "node:assert/strict";
import test from "node:test";

const { ApiError, NetworkError, ParseError } = await import(
	"../../resources/static/js/fetchClient.js"
);
const { handleApiError } = await import(
	"../../resources/static/js/errorHandler.js"
);

function capture(err) {
	const seen = { displayable: null, nonDisplayable: null };
	handleApiError(err, {
		onDisplayableMessage: (message) => {
			seen.displayable = message;
		},
		onNonDisplayable: (error) => {
			seen.nonDisplayable = error;
		},
	});
	return seen;
}

test("401 로그인 필요는 서버 문구를 그대로 보여 준다", () => {
	const seen = capture(
		new ApiError(401, "로그인이 필요합니다.", {
			message: "로그인이 필요합니다.",
			isDisplayableMessage: true,
		}),
	);
	assert.equal(seen.displayable, "로그인이 필요합니다.");
	assert.equal(seen.nonDisplayable, null);
});

test("400 검증 실패도 서버 문구를 그대로 보여 준다", () => {
	const seen = capture(
		new ApiError(400, "symbols is required", {
			message: "symbols is required",
			isDisplayableMessage: true,
		}),
	);
	assert.equal(seen.displayable, "symbols is required");
});

test("500 도 서버 문구를 그대로 보여 준다", () => {
	const message = "표시 순서를 저장하지 못했습니다. 다시 시도해 주세요.";
	const seen = capture(new ApiError(500, message, { message, isDisplayableMessage: true }));
	assert.equal(seen.displayable, message);
});

test("표시 가능 표시가 없으면 화면 기본 문구로 넘긴다", () => {
	// 표준 오류 본문(result 아래에 message, 키 이름도 displayableMessage)은 이 경로로 간다.
	const seen = capture(
		new ApiError(500, "Internal Server Error", {
			status: 500,
			title: "Internal Server Error",
			result: { message: "무언가 실패", displayableMessage: false },
		}),
	);
	assert.equal(seen.displayable, null, "표시 가능하지 않은 사유를 그대로 보여 주면 안 된다");
	assert.ok(seen.nonDisplayable instanceof ApiError);
});

test("네트워크·파싱 오류도 화면 기본 문구로 넘긴다", () => {
	assert.ok(capture(new NetworkError("timeout")).nonDisplayable instanceof NetworkError);
	assert.ok(capture(new ParseError("bad", "<html>")).nonDisplayable instanceof ParseError);
});

test("fetchJson 이 오류 메시지를 찾는 자리와 같다", () => {
	// fetchJson 은 ApiError(status, body?.message || statusText, body) 로 만든다.
	// 즉 최상위 message 를 쓴다. handleApiError 도 최상위를 읽으므로 둘이 같은 자리를 본다.
	const source = new ApiError(400, undefined, { message: "서버 사유" });
	assert.equal(capture(source).displayable, null, "isDisplayableMessage 가 없으면 표시하지 않는다");

	const displayable = new ApiError(400, undefined, {
		message: "서버 사유",
		isDisplayableMessage: true,
	});
	assert.equal(capture(displayable).displayable, "서버 사유");
});

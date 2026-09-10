import {
	ApiError,
	NetworkError,
	ParseError,
	BlueskyErrorMessage,
} from "./fetchClient.js";

export type DisplayHandler = (
	message: string,
	raw?: BlueskyErrorMessage | any,
) => void;
export type NonDisplayHandler = (err: any) => void;

/**
 * 서버 문구가 없을 때 쓰는 기본 안내. 화면 로케일(html lang)을 따른다.
 *
 * <p>로케일과 무관하게 한글을 띄우면 영어 화면에서 네트워크 오류 알림만 한글로 나온다(실측 2026-09-09, TS 스캔 4건).
 * 다른 화면 문구는 서버가 번들에서 채워 보내지만 이 넷은 서버 응답이 없거나 깨진 상황이라 클라이언트가 스스로 정해야 한다.
 */
export function defaultErrorText(
	kind: "generic" | "network" | "parse",
): string {
	const lang =
		typeof document !== "undefined" && document.documentElement
			? (document.documentElement.lang || "").toLowerCase()
			: "";
	const en = lang.indexOf("en") === 0;
	switch (kind) {
		case "network":
			return en
				? "A network error occurred. Check your internet connection."
				: "네트워크 오류가 발생했습니다. 인터넷 연결을 확인하세요.";
		case "parse":
			return en
				? "An error occurred while processing the server response."
				: "서버 응답을 처리하는 중 오류가 발생했습니다.";
		default:
			return en ? "An error occurred." : "오류가 발생했습니다.";
	}
}

/** 세션이 끊긴 뒤 돌아올 로그인 주소. 페이지·조각(loginRequiredView)과 같은 규약이다. */
export function loginUrlFor(currentHref: string): string {
	return "/login?redirectUrl=" + encodeURIComponent(currentHref);
}

/**
 * 401 이면 안내를 남긴 뒤 로그인으로 보낸다. 세션이 없으니 이 화면에서 더 할 수 있는 일이 없다.
 * 실측 2026-09-09: 월배당 프로필 순서 저장이 401 을 받으면 상태줄에 "로그인이 필요합니다" 만 남고 갈 길이 없었다.
 * 브라우저가 아닌 문맥(테스트)에서는 아무것도 하지 않는다.
 */
export function redirectToLoginIfUnauthorized(status: number): boolean {
	if (status !== 401) return false;
	if (typeof location === "undefined" || typeof location.assign !== "function") return false;
	location.assign(loginUrlFor(location.href));
	return true;
}

/**
 * 중앙화된 API 에러 핸들러
 * - isDisplayableMessage가 true인 경우 onDisplayableMessage로 전달 (없으면 alert)
 * - 아닌 경우 onNonDisplayable (있으면 전달) 또는 console.error
 */
export function handleApiError(
	err: any,
	{
		onDisplayableMessage,
		onNonDisplayable,
	}: {
		onDisplayableMessage?: DisplayHandler;
		onNonDisplayable?: NonDisplayHandler;
	} = {},
) {
	if (err instanceof ApiError) {
		const body = err.body as BlueskyErrorMessage | null;

		// 서버에서 BlueskyErrorMessage 형태로 보낸 경우
		if (body && body.isDisplayableMessage) {
			const msg = body.message || body.errorCode || defaultErrorText("generic");
			if (onDisplayableMessage) {
				onDisplayableMessage(msg, body);
			} else {
				// 기본 동작: alert
				alert(msg);
			}
			redirectToLoginIfUnauthorized(err.status);
			return;
		}

		// 문구가 없어도 401 은 로그인으로 보낸다.
		if (redirectToLoginIfUnauthorized(err.status)) {
			return;
		}
		// displayable이 아닌 경우: 상세 정보 전달
		if (onNonDisplayable) {
			onNonDisplayable(err);
		} else {
			console.error("API Error:", err.status, body ?? err.body ?? err.message);
		}
		return;
	}

	if (err instanceof NetworkError) {
		if (onNonDisplayable) onNonDisplayable(err);
		else alert(defaultErrorText("network"));
		return;
	}

	if (err instanceof ParseError) {
		if (onNonDisplayable) onNonDisplayable(err);
		else alert(defaultErrorText("parse"));
		return;
	}

	// 기타 예외
	if (onNonDisplayable) onNonDisplayable(err);
	else console.error("Unknown error", err);
}

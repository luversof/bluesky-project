"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleApiError = handleApiError;
const fetchClient_1 = require("./fetchClient");
/**
 * 중앙화된 API 에러 핸들러
 * - isDisplayableMessage가 true인 경우 onDisplayableMessage로 전달 (없으면 alert)
 * - 아닌 경우 onNonDisplayable (있으면 전달) 또는 console.error
 */
function handleApiError(err, { onDisplayableMessage, onNonDisplayable, } = {}) {
    var _a;
    if (err instanceof fetchClient_1.ApiError) {
        const body = err.body;
        // 서버에서 BlueskyErrorMessage 형태로 보낸 경우
        if (body && body.isDisplayableMessage) {
            const msg = body.message || body.errorCode || "오류가 발생했습니다.";
            if (onDisplayableMessage) {
                onDisplayableMessage(msg, body);
            }
            else {
                // 기본 동작: alert
                alert(msg);
            }
            return;
        }
        // displayable이 아닌 경우: 상세 정보 전달
        if (onNonDisplayable) {
            onNonDisplayable(err);
        }
        else {
            console.error("API Error:", err.status, (_a = body !== null && body !== void 0 ? body : err.body) !== null && _a !== void 0 ? _a : err.message);
        }
        return;
    }
    if (err instanceof fetchClient_1.NetworkError) {
        if (onNonDisplayable)
            onNonDisplayable(err);
        else
            alert("네트워크 오류가 발생했습니다. 인터넷 연결을 확인하세요.");
        return;
    }
    if (err instanceof fetchClient_1.ParseError) {
        if (onNonDisplayable)
            onNonDisplayable(err);
        else
            alert("서버 응답을 처리하는 중 오류가 발생했습니다.");
        return;
    }
    // 기타 예외
    if (onNonDisplayable)
        onNonDisplayable(err);
    else
        console.error("Unknown error", err);
}

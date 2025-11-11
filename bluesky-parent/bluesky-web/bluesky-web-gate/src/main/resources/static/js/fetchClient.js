// 공통 fetch 래퍼 및 에러 타입 정의
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
export class ApiError extends Error {
    constructor(status, message, body = null) {
        super(message || `HTTP ${status}`);
        this.status = status;
        this.body = body;
        Object.setPrototypeOf(this, ApiError.prototype);
    }
}
export class NetworkError extends Error {
    constructor(message) {
        super(message || "Network error");
        Object.setPrototypeOf(this, NetworkError.prototype);
    }
}
export class ParseError extends Error {
    constructor(message, raw) {
        super(message);
        this.raw = raw;
        Object.setPrototypeOf(this, ParseError.prototype);
    }
}
async function delay(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}
export async function fetchWithTimeout(input, init = {}, timeoutMs) {
    const controller = new AbortController();
    const signal = controller.signal;
    const finalInit = Object.assign(Object.assign({}, init), { signal });
    let timeoutId;
    if (typeof timeoutMs === "number" && timeoutMs > 0) {
        timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
    }
    try {
        const res = await fetch(input, finalInit);
        return res;
    }
    catch (err) {
        if (err && err.name === "AbortError") {
            throw new NetworkError("Request timed out or aborted");
        }
        throw new NetworkError((err === null || err === void 0 ? void 0 : err.message) || String(err));
    }
    finally {
        if (timeoutId)
            clearTimeout(timeoutId);
    }
}
/**
 * Generic JSON fetch helper with unified error handling.
 * - On non-2xx responses, try to parse body as BlueskyErrorMessage and throw ApiError
 */
export async function fetchJson(input, options = {}) {
    const { retries = 0, retryDelayMs = 500, parseJson = true, timeoutMs } = options, fetchInit = __rest(options, ["retries", "retryDelayMs", "parseJson", "timeoutMs"]);
    let attempt = 0;
    while (true) {
        try {
            const res = await fetchWithTimeout(input, fetchInit, timeoutMs);
            if (!res.ok) {
                // 서버가 에러를 보낸 경우, 가능한 경우 BlueskyErrorMessage로 파싱
                let body = null;
                try {
                    const text = await res.text();
                    if (text) {
                        try {
                            body = JSON.parse(text);
                        }
                        catch (e) {
                            // 파싱 실패 시 raw 텍스트 보관
                            body = text;
                        }
                    }
                }
                catch (e) {
                    body = null;
                }
                throw new ApiError(res.status, (body === null || body === void 0 ? void 0 : body.message) || res.statusText, body);
            }
            if (!parseJson) {
                // 호출자가 직접 처리를 원할 경우
                const txt = await res.text();
                return txt;
            }
            // No Content
            if (res.status === 204)
                return null;
            const text = await res.text();
            if (!text)
                return null;
            try {
                return JSON.parse(text);
            }
            catch (e) {
                throw new ParseError("Failed to parse JSON response", text);
            }
        }
        catch (err) {
            const isNetwork = err instanceof NetworkError;
            const isServerError = err instanceof ApiError && err.status >= 500 && err.status < 600;
            if ((isNetwork || isServerError) && attempt < retries) {
                attempt++;
                await delay(retryDelayMs * attempt);
                continue;
            }
            throw err;
        }
    }
}
export async function postJson(url, body, options = {}) {
    const headers = Object.assign({ "Content-Type": "application/json" }, (options.headers || {}));
    const init = Object.assign({ method: "POST", headers, body: JSON.stringify(body) }, options);
    return fetchJson(url, init);
}
export async function putJson(url, body, options = {}) {
    const headers = Object.assign({ "Content-Type": "application/json" }, (options.headers || {}));
    const init = Object.assign({ method: "PUT", headers, body: JSON.stringify(body) }, options);
    return fetchJson(url, init);
}
export async function deleteJson(url, body, options = {}) {
    const headers = Object.assign({ "Content-Type": "application/json" }, (options.headers || {}));
    const init = Object.assign({ method: "DELETE", headers, body: JSON.stringify(body) }, options);
    return fetchJson(url, init);
}

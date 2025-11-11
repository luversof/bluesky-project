// 공통 fetch 래퍼 및 에러 타입 정의

export interface BlueskyErrorMessage {
	errorCode?: string;
	errorMessageArgs?: string[];
	exceptionClassName?: string;
	isDisplayableMessage?: boolean;
	message?: string;
	object?: string;
	field?: string;
}

export class ApiError extends Error {
	public status: number;
	public body: BlueskyErrorMessage | any | null;

	constructor(
		status: number,
		message?: string,
		body: BlueskyErrorMessage | any | null = null,
	) {
		super(message || `HTTP ${status}`);
		this.status = status;
		this.body = body;
		Object.setPrototypeOf(this, ApiError.prototype);
	}
}

export class NetworkError extends Error {
	constructor(message?: string) {
		super(message || "Network error");
		Object.setPrototypeOf(this, NetworkError.prototype);
	}
}

export class ParseError extends Error {
	public raw: string;
	constructor(message: string, raw: string) {
		super(message);
		this.raw = raw;
		Object.setPrototypeOf(this, ParseError.prototype);
	}
}

async function delay(ms: number) {
	return new Promise((resolve) => setTimeout(resolve, ms));
}

export type FetchOptions = RequestInit & {
	timeoutMs?: number;
	retries?: number;
	retryDelayMs?: number;
	parseJson?: boolean; // 기본 true
};

export async function fetchWithTimeout(
	input: RequestInfo,
	init: RequestInit = {},
	timeoutMs?: number,
): Promise<Response> {
	const controller = new AbortController();
	const signal = controller.signal;
	const finalInit: RequestInit = { ...init, signal };

	let timeoutId: number | undefined;
	if (typeof timeoutMs === "number" && timeoutMs > 0) {
		timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
	}

	try {
		const res = await fetch(input, finalInit);
		return res;
	} catch (err: any) {
		if (err && err.name === "AbortError") {
			throw new NetworkError("Request timed out or aborted");
		}
		throw new NetworkError(err?.message || String(err));
	} finally {
		if (timeoutId) clearTimeout(timeoutId);
	}
}

/**
 * Generic JSON fetch helper with unified error handling.
 * - On non-2xx responses, try to parse body as BlueskyErrorMessage and throw ApiError
 */
export async function fetchJson<T = any>(
	input: RequestInfo,
	options: FetchOptions = {},
): Promise<T> {
	const {
		retries = 0,
		retryDelayMs = 500,
		parseJson = true,
		timeoutMs,
		...fetchInit
	} = options;

	let attempt = 0;
	while (true) {
		try {
			const res = await fetchWithTimeout(input, fetchInit, timeoutMs);

			if (!res.ok) {
				// 서버가 에러를 보낸 경우, 가능한 경우 BlueskyErrorMessage로 파싱
				let body: any = null;
				try {
					const text = await res.text();
					if (text) {
						try {
							body = JSON.parse(text);
						} catch (e) {
							// 파싱 실패 시 raw 텍스트 보관
							body = text;
						}
					}
				} catch (e) {
					body = null;
				}
				throw new ApiError(res.status, body?.message || res.statusText, body);
			}

			if (!parseJson) {
				// 호출자가 직접 처리를 원할 경우
				const txt = await res.text();
				return txt as unknown as T;
			}

			// No Content
			if (res.status === 204) return null as unknown as T;

			const text = await res.text();
			if (!text) return null as unknown as T;

			try {
				return JSON.parse(text) as T;
			} catch (e: any) {
				throw new ParseError("Failed to parse JSON response", text);
			}
		} catch (err: any) {
			const isNetwork = err instanceof NetworkError;
			const isServerError =
				err instanceof ApiError && err.status >= 500 && err.status < 600;

			if ((isNetwork || isServerError) && attempt < retries) {
				attempt++;
				await delay(retryDelayMs * attempt);
				continue;
			}

			throw err;
		}
	}
}

export async function postJson<T = any, U = any>(
	url: string,
	body: U,
	options: FetchOptions = {},
): Promise<T> {
	const headers = {
		"Content-Type": "application/json",
		...((options.headers || {}) as Record<string, string>),
	};

	const init: FetchOptions = {
		method: "POST",
		headers,
		body: JSON.stringify(body),
		...options,
	};

	return fetchJson<T>(url, init);
}

export async function putJson<T = any, U = any>(
	url: string,
	body: U,
	options: FetchOptions = {},
): Promise<T> {
	const headers = {
		"Content-Type": "application/json",
		...((options.headers || {}) as Record<string, string>),
	};

	const init: FetchOptions = {
		method: "PUT",
		headers,
		body: JSON.stringify(body),
		...options,
	};

	return fetchJson<T>(url, init);
}

export async function deleteJson<T = any, U = any>(
	url: string,
	body: U,
	options: FetchOptions = {},
): Promise<T> {
	const headers = {
		"Content-Type": "application/json",
		...((options.headers || {}) as Record<string, string>),
	};

	const init: FetchOptions = {
		method: "DELETE",
		headers,
		body: JSON.stringify(body),
		...options,
	};

	return fetchJson<T>(url, init);
}

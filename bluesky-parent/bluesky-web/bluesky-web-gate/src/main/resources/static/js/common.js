
// url query parameter 처리
const param = (() => {
	let _params = new URLSearchParams(globalThis.location.search);

	return {
		refreshUrl() {
			globalThis.history.replaceState(null, null, "?" + _params.toString());
		},
		getParams() {
			return _params;
		},
		getParam(paramKey) {
			if (_params.get(paramKey) == '') _params.delete(paramKey)
			return _params.get(paramKey) == null ? null : _params.get(paramKey);
		},
		setParam(paramKey, paramValue) {
			if (paramValue == null || paramValue == '') {
				_params.delete(paramKey);
			} else {
				_params.set(paramKey, paramValue);
			}
			this.refreshUrl();
		},
		deleteParam(paramKey) {
			_params.delete(paramKey);
		},
		resetParam() {
			_params = new URLSearchParams();
			this.refreshUrl();
		},
		getRequestPage() {
			let page = this.getParam("page");
			return (page == null ? 1 : page) - 1;
		}
	}
})();

// 공통 에러 처리? 에러 발생 시 노출 가능 여부에 따라 노출을 어떻게 할지 처리
const errorHandler = (() => {
	return {
		handle(response) {
			
			if (response.ok) {
				return response;
			}
			
			console.log("response error : {}", response.json())
		}
	}
})();


document.addEventListener('htmx:beforeSwap', (event) => {
	if('hx-indicator' in event.target.attributes) {
		let indicator = document.querySelector(event.target.getAttribute('hx-indicator')).cloneNode(true);
		indicator.style.display = "block";
		event.target.innerHTML = "";
		event.target.appendChild(indicator);
	}
});
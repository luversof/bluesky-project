var param = (function() {
	var _params = new URLSearchParams(window.location.search);

	return {
		refreshUrl() {
			window.history.replaceState(null, null, "?" + _params.toString());
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
				console.log("delete param", paramKey)
				_params.delete(paramKey);
			} else {
				_params.set(paramKey, paramValue);
			}
			this.refreshUrl();
		},
		resetParam() {
			// 이거 특정 위치에 초기화 처리하는 방식으로 변경해야함
			this.setParam("product", null);
			this.setParam("mainMenu", null);
			this.setParam("subMenu", null);
			if (document.querySelector("[name=product]")) document.querySelector("[name=product]").value = "";
			if (document.querySelector("[name=mainMenu]")) document.querySelector("[name=mainMenu]").value = "";
			if (document.querySelector("[name=subMenu]")) document.querySelector("[name=subMenu]").value = "";
		},
		getRequestPage() {
			var page = this.getParam("page");
			return (page == null ? 1 : page) - 1;
		}
	}
})();


// htmx modal open 처리
document.addEventListener('htmx:afterRequest', function(event) {
	if (event.target.dataset.modalTarget) {
		eval(event.target.dataset.modalTarget).showModal()
	}
});


window.onload = function() {
	// 상단 메뉴의 링크에 검색 parameter를 추가 처리
	document.querySelectorAll(".navbar .menu a").forEach(el => el.addEventListener("click", (event) => {
		var url = new URL(event.target.href);
		param.getParams().forEach((value, key) => {
			url.searchParams.set(key, value);
		})
		// page parameter는 제거
		url.searchParams.delete("page");
		el.setAttribute("href", url);
		return false;
	}));
	
	// 검색 변경 이벤트
	document.querySelectorAll(".search .searchinput").forEach(el => el.addEventListener("change", (event) => param.setParam(event.target.name, event.target.value)));
	
	// 검색 reset 버튼 이벤트
	document.querySelectorAll(".search .resetbutton").forEach(el => el.addEventListener("click", () => param.resetParam()));
	
}
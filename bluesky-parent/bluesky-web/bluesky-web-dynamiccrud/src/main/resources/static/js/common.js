var searchInputSelector = ".search .searchinput";

var param = (() => {
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
			document.querySelectorAll(searchInputSelector).forEach(el => el.value = "");
			_params = new URLSearchParams();
			this.refreshUrl();
		},
		getRequestPage() {
			var page = this.getParam("page");
			return (page == null ? 1 : page) - 1;
		}
	}
})();


// htmx trigger로 로딩된 이후 modal을 띄우거나 종료 처리
document.addEventListener('htmx:afterRequest', (event) => {
	// data-modal-target="대상modal" 있으면 관련하여 modal 처리
	if (event.target.dataset.modalTarget) {
		if (event.target.dataset.modalAction == "showModal") {
			var modalTarget = eval(event.target.dataset.modalTarget);
			// modal 생성 시 해당 버튼에 hidden input이 있으면 해당 값을 modal에 설정 
			event.target.querySelectorAll("input[type=hidden]").forEach(el => {
				modalTarget.querySelector("[name=" + el.name  +"]").value = el.value;
			});
			
			modalTarget.showModal();
		} else if(event.target.dataset.modalAction == "close") {
			// 이거 페이지 첫번째 페이지로 이동을 해줘야 하나? (데이터 생성 후 닫는 경우만 해당..)
			eval(event.target.dataset.modalTarget).close();
		}
	}
});

// modal에서 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 요청 시 해당 값 사용  
document.addEventListener('createModal', () => param.setParam("page", 1));

window.addEventListener('load', () => {
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
	
	// 검색 변경 이벤트, 변경 시 page는 1로 리셋한다.
	document.querySelectorAll(searchInputSelector).forEach(el => el.addEventListener("change", (event) => {
		param.setParam("page", 1);
		param.setParam(event.target.name, event.target.value);
	}));
	
	// 검색 reset 버튼 이벤트
	document.querySelectorAll(".search .resetbutton").forEach(el => el.addEventListener("click", () => param.resetParam()));
});

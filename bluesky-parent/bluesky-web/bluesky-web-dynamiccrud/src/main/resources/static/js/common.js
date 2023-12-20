var searchInputSelector = "#searchArea .searchinput";

// url query parameter 처리
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

/** modalForm에 field type에 따라 input이 생성됨. type에 따라 알맞게 데이터를 설정 */
function copyContentDataToModal(contentDataEl, modalTarget) {
	contentDataEl.querySelectorAll("input[type=hidden]").forEach(el => {
		var targetInput = modalTarget.querySelector("[name=" + el.name + "]");
		targetInput.value = el.value;
		if (!el.name.startsWith("__org__")) {
			var checkBoxInput = targetInput.closest("div").querySelector("input[type=checkbox]");
			if (checkBoxInput != null) {
				checkBoxInput.checked = eval(el.value);
			}
		}
	});
}

// 이거 굳이 필요한가?
function copySearchAreaToModal(modalTarget) {
	document.querySelectorAll("#searchArea .searchinput").forEach(el => {
		modalTarget.querySelector("[name=" + el.name  +"]").value = el.value;
	});
}


function deleteData(event) {
		// 삭제 대상이 선택되어 있는지 확인
		var checkedList = document.querySelector("#contentTable").querySelectorAll("input[name=contentDataCheck]:checked")
		if (checkedList.length <= 0) {
			alert("삭제할 행을 체크해주세요.");
			event.preventDefault();
			return;
		}
		if (!confirm("선택한 항목을 정말 삭제하시겠습니까?")) {
			console.log("테스트")
			event.preventDefault();
			return;
		}
		// 선택 대상들의 input 데이터를 submit 해야 함
		
}

document.addEventListener('htmx:beforeRequest', (event) => {
	
	/** (s) deleteButton의 경우 처리 */
	if (event.target.classList.contains("deleteButton")) {
		deleteData(event);
	}
});
// htmx trigger로 로딩된 이후 modal을 띄우거나 종료 처리
document.addEventListener('htmx:afterRequest', (event) => {
	/** (s) data-modal-target="대상modal" 속성이 있는 경우 modal 관련 처리 수행 */ 
	if (event.target.dataset.modalTarget) {
		if (event.target.dataset.modalAction == "showModal") {
			var modalTarget = eval(event.target.dataset.modalTarget);
			// modal 생성 시 요청한 버튼에 hidden input이 있으면 해당 값을 modal에 설정
			if (event.target.closest("tr")) {
				copyContentDataToModal(event.target.closest("tr").querySelector(".contentData"), modalTarget)
			}
			
			/** (s) 데이터 복사 eventListener */
			//modal 관련 eventListener 등록
			// contentTable 영역에 선택한 체크박스 라인의 데이터를 #modalForm의 input 으로 가져온다.
			modalTarget.querySelector(".copyDataButton").addEventListener("click", () => {
				var checkedList = document.querySelector("#contentTable").querySelectorAll("input[name=contentDataCheck]:checked")
				if (checkedList.length <= 0) {
					alert("복사할 행을 체크해주세요.");
					return;
				}
				copyContentDataToModal(checkedList[0].closest("tr").querySelector(".contentData"), modalTarget)
			})
			/** (e) 데이터 복사 eventListener */

			/** (s) 검색 복사 eventListener */			
			modalTarget.querySelector(".copySearchButton").addEventListener("click", () => copySearchAreaToModal(modalTarget));
			/** (e) 검색 복사 eventListener */
			
			
			/** (s) modal의 checkBox의 on/off 값 설정 eventListener */
			modalTarget.querySelectorAll('input[type=checkbox]').forEach(el => el.addEventListener("change", (event) => 
				event.target.closest('div').querySelector("input[type=hidden]").value = event.target.checked ? true : false
			));
			
			/** (e) modal의 checkBox의 on/off 값 설정 eventListener */
			
			modalTarget.showModal();
		} else if(event.target.dataset.modalAction == "close") {
			// 이거 페이지 첫번째 페이지로 이동을 해줘야 하나? (데이터 생성 후 닫는 경우만 해당..)
			eval(event.target.dataset.modalTarget).close();
		}
	}
	/** (e) data-modal-target="대상modal" 속성이 있는 경우 modal 관련 처리 수행 */
	
	/** 체크박스 선택 처리 */
	event.target.querySelectorAll("input[name=contentDataCheck]").forEach(el => el.addEventListener("change", (event) => {
		if (event.target.checked) {
			event.target.closest("tr").classList.add("active");
		} else {
			event.target.closest("tr").classList.remove("active");		
		}
	}));
	
	event.target.querySelectorAll("input[name=contentDataCheckToggle]").forEach(el => el.addEventListener("change", (event) => {
		event.target.closest("table").querySelector("tbody").querySelectorAll("input[name=contentDataCheck]").forEach(el => {
			el.checked = event.target.checked;
			el.dispatchEvent(new Event("change"));
		});
	}));
});

// modal에서 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 갱신 시 첫 페이지로 이동 처리  
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
	document.querySelectorAll("#searchArea .resetbutton").forEach(el => el.addEventListener("click", () => param.resetParam()));
});

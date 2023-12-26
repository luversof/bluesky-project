var searchInputSelector = "#searchArea .searchinput";
var modalSelector = "#modal";

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
function copyContentDataToModalForm(contentDataEl, modalTarget) {
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

// 검색 내용을 modalForm으로 복사
function copySearchAreaToModalForm(modalTarget) {
	document.querySelectorAll("#searchArea .searchinput").forEach(el => {
		modalTarget.querySelector("[name=" + el.name  +"]").value = el.value;
	});
}


function checkDeleteData(event) {
	// 삭제 대상이 선택되어 있는지 확인
	var checkedList = document.querySelector("#contentTable").querySelectorAll("input[name=contentDataCheck]:checked")
	if (checkedList.length <= 0) {
		alert("삭제할 행을 체크해주세요.");
		event.preventDefault();
		return;
	}
	if (!confirm("선택한 항목을 정말 삭제하시겠습니까?")) {
		event.preventDefault();
		return;
	}
}

function checkExportData(event) {
	// 삭제 대상이 선택되어 있는지 확인
	var checkedList = document.querySelector("#contentTable").querySelectorAll("input[name=contentDataCheck]:checked")
	if (checkedList.length <= 0) {
		alert("export할 행을 체크해주세요.");
		event.preventDefault();
		return;
	}
}

// htmx에서 request를 발생하기 전 사전 처리가 필요한 항목들에 대한 설정
// 더 나은 방법은 없나?
document.addEventListener('htmx:beforeRequest', (event) => {
	/** (s) deleteButton 처리 */
	if (event.target.classList.contains("deleteButton")) {
		checkDeleteData(event);
	}
	/** (e) deleteButton 처리 */
	
	/** (s) exportButton 처리 */
	if (event.target.classList.contains("exportButton")) {
		checkExportData(event);
	}
	/** (e) exportButton 처리 */
});

document.addEventListener('showList', () => 
	setTimeout(() => {
		var target = document.querySelector("#contentTable");
			/** 체크박스 선택 처리 */
		target.querySelectorAll("input[name=contentDataCheck]").forEach(el => el.addEventListener("change", (event) => {
			if (event.target.checked) {
				event.target.closest("tr").classList.add("active");
			} else {
				event.target.closest("tr").classList.remove("active");		
			}
		}));
		
		target.querySelectorAll("input[name=contentDataCheckToggle]").forEach(el => el.addEventListener("change", (event) => {
			event.target.closest("table").querySelector("tbody").querySelectorAll("input[name=contentDataCheck]").forEach(el => {
				el.checked = event.target.checked;
				el.dispatchEvent(new Event("change"));
			});
		}));
	}, 1)
);

document.addEventListener('showCreateModalForm', () => 
	setTimeout(() => {
		var modalTarget = document.querySelector(modalSelector);	
		
		/** (s) 데이터 복사 eventListener */
		// contentTable 영역에 선택한 체크박스 라인의 데이터를 #modalForm의 input 으로 가져온다.
		modalTarget.querySelector(".copyDataButton").addEventListener("click", () => {
			var checkedList = document.querySelector("#contentTable").querySelectorAll("input[name=contentDataCheck]:checked")
			if (checkedList.length <= 0) {
				alert("복사할 행을 체크해주세요.");
				return;
			}
			copyContentDataToModalForm(checkedList[0].closest("tr").querySelector(".contentData"), modalTarget)
		})
		/** (e) 데이터 복사 eventListener */

		/** (s) 검색 복사 eventListener */			
		modalTarget.querySelector(".copySearchButton").addEventListener("click", () => copySearchAreaToModalForm(modalTarget));
		/** (e) 검색 복사 eventListener */
		
		
		/** (s) modal의 checkBox의 on/off 값 설정 eventListener */
		modalTarget.querySelectorAll('input[type=checkbox]').forEach(el => el.addEventListener("change", (event) => 
			event.target.closest('div').querySelector("input[type=hidden]").value = event.target.checked ? true : false
		));
		
		/** (e) modal의 checkBox의 on/off 값 설정 eventListener */
		
		modalTarget.showModal();
		
	}, 1)
);

document.addEventListener('showUpdateModalForm', (event) => {
	setTimeout(() => {
		var modalTarget = document.querySelector(modalSelector);
		copyContentDataToModalForm(event.target.closest("tr").querySelector(".contentData"), modalTarget)
		modalTarget.showModal();
	}, 1)
});

// modalForm에 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 갱신 시 첫 페이지로 이동 처리  
document.addEventListener('createModalForm', () => param.setParam("page", 1));

document.addEventListener('closeModalForm', () => {
	setTimeout(() => {
		document.querySelector(modalSelector).close();
	}, 1)
});


document.addEventListener('showExportModalBulkForm', () => {
	setTimeout(() => {
		var modalTarget = document.querySelector(modalSelector);
		var checkedIds = [...document.querySelectorAll("#contentTable input[name=contentDataCheck]")].filter(el => el.checked).map(el => parseInt(el.value));
		var targetList = contentList.filter((_el, index) => checkedIds.includes(index));
		modalTarget.querySelector("textarea").value = JSON.stringify(targetList);
		
		// copyButton eventListener
		/** (s) 데이터 복사 eventListener */
		// textArea의 데이터를 clipboard로 복사한다.
		modalTarget.querySelector(".copyDataButton").addEventListener("click", () => {
			if (navigator.clipboard == undefined) {
				alert("클립보드 복사가 지원되지 않는 환경입니다.\n직접 복사하여 사용하세요.")
			} else {
				navigator.clipboard.writeText(modalTarget.querySelector("textarea").value).then(() => {
					alert("데이터를 clipboard에 복사하였습니다.");
			    }).catch(err => {
			        console.log('클립보드 복사 실패', err);
			    })
			}
		});
		
		/** (e) 데이터 복사 eventListener */		
		
		modalTarget.showModal();
	}, 1)
});

document.addEventListener('showImportModalBulkForm', () => {
	setTimeout(() => {
		var modalTarget = document.querySelector(modalSelector);
		modalTarget.showModal();
	}, 1)
});

// modalForm에 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 갱신 시 첫 페이지로 이동 처리  
document.addEventListener('importModalBulkForm', () => param.setParam("page", 1));


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

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
				_params.delete(paramKey);
			} else {
				_params.set(paramKey, paramValue);
			}
			this.refreshUrl();
		},
		resetParam() {
			_params = new URLSearchParams();
			this.refreshUrl();
		},
		getRequestPage() {
			var page = this.getParam("page");
			return (page == null ? 1 : page) - 1;
		}
	}
})();

var navbarAreaFn = (() => {
	return {
		addEventListener() {
			// 상단 메뉴의 링크에 검색 parameter를 추가 처리
			document.querySelectorAll(".navbar .menu a").forEach(el => el.addEventListener("click", (event) => {
				var url = new URL(event.target.href);
				param.getParams().forEach((value, key) => url.searchParams.set(key, value))
				// page parameter는 제거
				url.searchParams.delete("page");
				el.setAttribute("href", url);
				return false;
			}));
		}
	}
})();

var searchAreaFn = (() => {
	var _searchAreaSelector = "#searchArea";
	var _searchInputArea = _searchAreaSelector + " .searchInput";
	var _searchButtonArea = _searchAreaSelector + " .searchButton";
	var _resetButtonArea = _searchAreaSelector + " .resetButton";
	
	return {
		addEventListener() {
			// 검색 변경 이벤트, 변경 시 page는 1로 리셋한다.
			document.querySelectorAll(_searchInputArea).forEach(el => el.addEventListener("change", (event) => {
				searchAreaFn.searchList(event);
			}));
			document.querySelectorAll(_searchButtonArea).forEach(el => el.addEventListener("click", () => {
				searchAreaFn.searchList();
			}));
			
			// 검색 reset 버튼 이벤트
			document.querySelectorAll(_resetButtonArea).forEach(el => el.addEventListener("click", () => searchAreaFn.reset()));
			
		},
		// 초기화 (페이지 리로드 시 검색 영역과 param 설정 일치 처리)
		initialize() {
			document.querySelectorAll(_searchInputArea).forEach(el => {
				if (el.value) {
					param.setParam(el.name, el.value);
				}
			});
		},
		// 초기화 버튼 액션 처리 - 기본값을 처리해야 할까?
		reset() {
			document.querySelectorAll(_searchInputArea).forEach(el => el.value = "");
			param.resetParam();
		},
		// 검색 영역 목록 검색, page는 1로 리셋한다.
		searchList(event) {
			var isValid = true;
			document.querySelectorAll(_searchInputArea).forEach(el => {
				if (!isValid) {
					return;
				}
				
				if (!el.checkValidity()) {
					el.reportValidity();
					isValid = false;
					return;
				}
			});
			
			if (!isValid) {
				return;
			}
			
			if (event) {
				param.setParam("page", 1);
				param.setParam(event.target.name, event.target.value);
			}
			htmx.trigger("#listArea", "searchListTrigger");
		}
	}
})();

var listAreaFn = (() => {
	var _listAreaSelector = "#content";
	
	return {
		// response header로 전달받은 HX-Trigger에 대한 event trigger
		addEventListener() {
			document.addEventListener('showList', () => {
				var targetArea = document.querySelector(_listAreaSelector);
				/** 체크박스 선택 처리 */
				targetArea.querySelectorAll("#contentTable input[name=contentDataCheck]").forEach(el => el.addEventListener("change", (event) => {
					if (event.target.checked) {
						event.target.closest("tr").classList.add("active");
					} else {
						event.target.closest("tr").classList.remove("active");		
					}
				}));
				
				targetArea.querySelectorAll("#contentTable input[name=contentDataCheckToggle]").forEach(el => el.addEventListener("change", (event) => {
					event.target.closest("table").querySelector("tbody").querySelectorAll("input[name=contentDataCheck]").forEach(el => {
						el.checked = event.target.checked;
						el.dispatchEvent(new Event("change"));
					});
				}));
				/** 링크에 class="link" 추가 처리 */
				targetArea.querySelectorAll("#contentTable tbody td a").forEach(el => {
					el.classList.add("btn");
					el.classList.add("btn-sm");
					el.innerHTML = "<svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke-width='1.5' stroke='currentColor' class='w-6 h-6'><path stroke-linecap='round' stroke-linejoin='round' d='M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m13.35-.622 1.757-1.757a4.5 4.5 0 0 0-6.364-6.364l-4.5 4.5a4.5 4.5 0 0 0 1.242 7.244' /></svg>";
				});
				
			
				/** (s) deleteButton 처리 */
				targetArea.querySelectorAll(".deleteButton").forEach(el => el.addEventListener("click", (event) => this.checkDeleteData(event)));
				/** (e) deleteButton 처리 */
				
				/** (s) exportButton 처리 */
				targetArea.querySelectorAll(".exportButton").forEach(el => el.addEventListener("click", (event) => this.checkExportData(event)));
				/** (e) exportButton 처리 */
			});
			

		},
		checkDeleteData(event) {
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
			htmx.trigger(_listAreaSelector + " .deleteButton", "deleteData");
		},
		checkExportData(event) {
			var checkedList = document.querySelector("#contentTable").querySelectorAll("input[name=contentDataCheck]:checked")
			if (checkedList.length <= 0) {
				alert("export할 행을 체크해주세요");
				event.preventDefault();
				return;
			}
			htmx.trigger(_listAreaSelector + " .exportButton", "exportData");
		}
	}
})();


var modalFormFn = (() => {
	var _modalAreaSelector = "#modal";
	
	return {
		// response header로 전달받은 HX-Trigger에 대한 event trigger
		addEventListener() {
			document.addEventListener('showModalForm', () => document.querySelector(_modalAreaSelector).showModal());
			document.addEventListener('closeModalForm', () => document.querySelector(_modalAreaSelector).close());
			document.addEventListener('createModalForm', () => this.initialize());
			document.addEventListener('updateModalForm', (event) => {
				this.initialize();
				this.copyContentDataToModalForm(event.target.closest("tr").querySelector(".contentData"))
			});
			
			// modalForm에 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 갱신 시 첫 페이지로 이동 처리  
			document.addEventListener('createModal', () => param.setParam("page", 1));
			
			document.addEventListener('exportModalBulkForm', () => {
				var modalTarget = document.querySelector(_modalAreaSelector);
				var checkedIds = [...document.querySelectorAll("#contentTable input[name=contentDataCheck]")].filter(el => el.checked).map(el => parseInt(el.value));
				var targetList = contentList.filter((_el, index) => checkedIds.includes(index));
				modalTarget.querySelector("textarea").value = JSON.stringify(targetList);
				
				/** (s) 데이터 복사 eventListener */
				modalTarget.querySelector(".copyDataButton").addEventListener("click", () => {
					if (navigator.clipboard == undefined) {
						alert("클립보드 복사가 지원되지 않는 환경입니다.\n직접 복사하여 사용하세요.");
					} else {
						navigator.clipboard.writeText(modalTarget.querySelector("textarea").value).then(() => {
							alert("데이터를 clipboard에 복사하였습니다.");
					    }).catch(err => {
					        console.log('클립보드 복사 실패', err);
					    })
					}
				});
				/** (e) 데이터 복사 eventListener */		
			});
			
			// modalForm에 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 갱신 시 첫 페이지로 이동 처리  
			document.addEventListener('importModalBulk', () => param.setParam("page", 1));
		},
		/** modalForm에 field type에 따라 input이 생성됨. type에 따라 알맞게 데이터를 설정 */
		// contentDataEl의 hidden input을 그대로 복사하여 설정, 동일 이름이 있으면 값을 추가하도록 처리
		copyContentDataToModalForm(contentDataEl) {
			var modalTarget = document.querySelector(_modalAreaSelector);
			contentDataEl.querySelectorAll("input[type=hidden]").forEach(el => {
				var targetInput = modalTarget.querySelector("[name=" + el.name + "]");
				if (targetInput === null) {
					modalTarget.querySelector("#modalForm").appendChild(el.cloneNode(true));
				} else {
					targetInput.value = el.value;
					// 체크박스는 input과 체크박스 표시가 별도로 존재하여 추가 처리 필요
					var checkBoxInput = targetInput.parentElement.querySelector("input[type=checkbox][name=" + (targetInput.name + "_checkbox") + "]");
					if (targetInput.parentElement.classList.contains("join") && checkBoxInput != null) {
						checkBoxInput.checked = eval(el.value);
					}
				}
			});
		},
		/** 검색 내용을 modalForm으로 복사 */
		copySearchAreaToModalForm(modalTarget) {
			document.querySelectorAll("#searchArea .searchInput").forEach(el => modalTarget.querySelector("[name=" + el.name  +"]").value = el.value);
		},
		/** modalForm 영역 초기 처리 */
		initialize() {
			var modalTarget = document.querySelector(_modalAreaSelector);
			
			/** (s) 데이터 복사 eventListener */
			// contentTable 영역에 선택한 체크박스 라인의 데이터를 #modalForm의 input 으로 가져온다.
			modalTarget.querySelector(".copyDataButton").addEventListener("click", () => {
				var checkedList = document.querySelector("#contentTable").querySelectorAll("input[name=contentDataCheck]:checked")
				if (checkedList.length <= 0) {
					alert("복사할 행을 체크해주세요.");
					return;
				}
				this.copyContentDataToModalForm(checkedList[0].closest("tr").querySelector(".contentData"));
			})
			/** (e) 데이터 복사 eventListener */
	
			/** (s) 검색 복사 eventListener */			
			modalTarget.querySelector(".copySearchButton").addEventListener("click", () => this.copySearchAreaToModalForm(modalTarget));
			/** (e) 검색 복사 eventListener */
			
			
			/** (s) modal의 checkBox의 on/off 값 설정 eventListener */
			modalTarget.querySelectorAll('#modalForm input[type=checkbox]').forEach(el => el.addEventListener("change", (event) =>
				event.target.closest('div').querySelector("input[type=hidden][name=" + event.target.name.replace('_checkbox', '') + "]").value = event.target.checked ? true : false
			));
			/** (e) modal의 checkBox의 on/off 값 설정 eventListener */
			
			/** (s) group 별 input 목록 취합 */
			var columnGroupIdList = [];
			document.querySelectorAll("[data-columngroupid]").forEach(el => columnGroupIdList.push(el.dataset.columngroupid));
			columnGroupIdList = columnGroupIdList.filter((item, pos) => columnGroupIdList.indexOf(item) === pos);
			columnGroupIdList.forEach(columnGroupId => {
				var targetEl;	
				document.querySelectorAll("[data-columngroupid=" + columnGroupId + "]").forEach((el, index) => {
					if (index == 0) {
						targetEl = el;
					} else {
						targetEl.appendChild(el.querySelector("label"));
					}
				});
			});
			/** (e) group 별 input 목록 취합 */
			
			/** (s) input의 tooltip의 위치 설정 */
			var modalBoxRect = modalTarget.querySelector(".modal-box").getBoundingClientRect();
			
			// model 위치
			var modalBoxXStart = modalBoxRect.x;
			var modalBoxXEnd = modalBoxRect.x + modalBoxRect.width;
			var modalBoxYStart = modalBoxRect.y;
			var modalBoxYEnd = modalBoxRect.y + modalBoxRect.height;
			
			modalTarget.querySelectorAll(".modal-box .tooltip").forEach(el => {
				// tooltip class 위치를 확인하여 tooltip의 방향 설정 class 추가
				var targetRect = el.getBoundingClientRect();
				
				var leftGap = targetRect.x - modalBoxXStart;
				var rightGap = modalBoxXEnd - targetRect.x;
				var topGap = targetRect.y - modalBoxYStart;
				var bottomGap = modalBoxYEnd - targetRect.y;
				
				// tooltip의 기본 너비 : 320, 오른쪽 간격이 320 이상이면 오른쪽 배치
				el.classList.add(rightGap > 320 ? "tooltip-right" : leftGap > 320 ? "tooltip-left" : topGap > bottomGap ? "tooltip-top" : "tooltip-bottom");
			
			});
			/** (e) input의 tooltip의 위치 설정 */
		}
	}
})();

listAreaFn.addEventListener();
modalFormFn.addEventListener();

window.addEventListener('DOMContentLoaded', () => {
	navbarAreaFn.addEventListener();
	
	// 초기화
	searchAreaFn.addEventListener();
	searchAreaFn.initialize();
});

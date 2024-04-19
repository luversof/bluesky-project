// url query parameter 처리
const param = (() => {
	let _params = new URLSearchParams(window.location.search);

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

const navbarAreaFn = (() => {
	return {
		addEventListener() {
			// 상단 메뉴의 링크에 검색 parameter를 추가 처리
			document.querySelectorAll(".navbar .navbar-center .menu a").forEach(el => el.addEventListener("click", (event) => {
				const url = new URL(event.target.href);
				param.getParams().forEach((value, key) => url.searchParams.set(key, value))
				// page parameter는 제거
				url.searchParams.delete("page");
				el.setAttribute("href", url);
				return false;
			}));
		}
	}
})();

const searchAreaFn = (() => {
	const _searchAreaSelector = "#searchArea";
	const _searchInputArea = _searchAreaSelector + " .searchInput";
	const _searchButtonArea = _searchAreaSelector + " .searchButton";
	const _resetButtonArea = _searchAreaSelector + " .resetButton";
	
	return {
		addEventListener() {
			document.querySelectorAll(_searchInputArea).forEach(el => el.addEventListener("change", (event) => searchAreaFn.searchList(event)));
			document.querySelectorAll(_searchButtonArea).forEach(el => el.addEventListener("click", () => searchAreaFn.searchList()));
			document.querySelectorAll(_resetButtonArea).forEach(el => el.addEventListener("click", () => {
				searchAreaFn.reset();
				searchAreaFn.searchList();
			}));
			
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
			let isValid = true;
			document.querySelectorAll(_searchInputArea).forEach(el => {
				if (!isValid) {
					return;
				}
				
				if (!el.checkValidity()) {
					el.reportValidity();
					isValid = false;
				}
			});
			
			if (!isValid) {
				return;
			}
			
			if (event) {
				// event가 있는 경우 (input 변경 시 호출의 경우) page를 1로 변경하고 param 값을 추가
				param.setParam("page", 1);
				param.setParam(event.target.name, event.target.value);
			}
			htmx.trigger("#listArea", "listFragmentTrigger");
		}
	}
})();

const listAreaFn = (() => {
	const _listAreaSelector = "#content";
	
	function _getTableCells() {
		let rows = document.querySelectorAll(_listAreaSelector + " tbody tr");
		let cells = [];
		for (let i = 0 ; i < rows.length ; i++) {
			cells[i] = rows[i].querySelectorAll("td:not(.contentData,.contentDataMenu,.contentDataCheck)");
		}
		return cells;
	}
	
	return {
		// response header로 전달받은 HX-Trigger에 대한 event trigger
		addEventListener() {
			document.addEventListener('listFragmentResponseTrigger', () => {
				const targetArea = document.querySelector(_listAreaSelector);
				/** 체크박스 선택 처리 */
				targetArea.querySelectorAll("#contentTable input[name=contentDataCheck]").forEach(el => el.addEventListener("change", (event) => {
					if (event.target.checked) {
						event.target.closest("tr").classList.add("active");
					} else {
						event.target.closest("tr").classList.remove("active");		
					}
				}));
				
				targetArea.querySelectorAll("#contentTable input[name=contentDataCheckToggle]").forEach(el => el.addEventListener("change", (event) => 
					event.target.closest("table").querySelectorAll("tbody input[name=contentDataCheck]").forEach(el => {
						el.checked = event.target.checked;
						el.dispatchEvent(new Event("change"));
					})
				));
				/** 링크에 class="link" 추가 처리 */
				targetArea.querySelectorAll("#contentTable tbody td a").forEach(el => {
					el.classList.add("btn");
					el.classList.add("btn-sm");
					el.innerHTML = "<svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke-width='1.5' stroke='currentColor' class='w-6 h-6'><path stroke-linecap='round' stroke-linejoin='round' d='M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m13.35-.622 1.757-1.757a4.5 4.5 0 0 0-6.364-6.364l-4.5 4.5a4.5 4.5 0 0 0 1.242 7.244' /></svg>";
				});
				
				/** (s) navButton 처리 */
				targetArea.querySelectorAll(".navButton").forEach(el => el.addEventListener("click", (event) => {
					param.setParam("page", event.target.dataset.page);
					htmx.trigger("#listArea", "listFragmentTrigger");
				}));
				/** (e) navButton 처리 */
			
				/** (s) deleteButton 처리 */
				targetArea.querySelectorAll(".deleteButton").forEach(el => el.addEventListener("click", (event) => this.checkDeleteData(event)));
				/** (e) deleteButton 처리 */
				
				/** (s) exportButton 처리 */
				targetArea.querySelectorAll(".exportButton").forEach(el => el.addEventListener("click", (event) => this.checkExportData(event)));
				/** (e) exportButton 처리 */
				
				/** (s) rowSpan 처리 */
				targetArea.querySelector("#useListAreaRowSpan").addEventListener("click", () => this.toggleRowSpan());
				this.setRowSpan(this.getRowSpan());
				/** (e) rowSpan 처리 */
			});
			

		},
		getRowSpan() {
			let useListAreaRowSpan = localStorage.getItem("useListAreaRowSpan");
			return (useListAreaRowSpan != null && useListAreaRowSpan == "true");
		},
		toggleRowSpan() {
			this.setRowSpan(!this.getRowSpan());
		},
		setRowSpan(useListAreaRowSpan) {
			localStorage.setItem("useListAreaRowSpan", useListAreaRowSpan);
			const useListAreaRowSpanArea = document.querySelector("#useListAreaRowSpan");
			
			if (useListAreaRowSpan) {
				useListAreaRowSpanArea.classList.add("swap-active");
				this.appendRowSpan();
			} else {
				useListAreaRowSpanArea.classList.remove("swap-active");
				this.removeRowSpan();
			}
		},
		appendRowSpan() {
			const cells = _getTableCells();
			// rowspan 적용
			for (let i = 0; i < cells.length ; i++) {
				for (let j = 0; j < cells[i].length ; j++) {
					if (cells[i][j].classList.contains("hidden")) {
						continue;
					}
					
					let spanCount = 1;
					let isTarget = true;
					for (let k = i + 1; k < cells.length ; k++) {
						if (!isTarget) {
							break;
						}
						if (cells[i][j].textContent == cells[k][j].textContent) {
							spanCount ++;
							cells[k][j].classList.add("hidden");
						} else {
							isTarget = false;
						}
					}
					
					if (spanCount > 1) {
						cells[i][j].setAttribute("rowspan", spanCount);
					}
				}
			}
		},
		removeRowSpan() {
			const cells = _getTableCells();
			cells.forEach(rows => rows.forEach(element => {
				if (element.hasAttribute("rowspan")) {
					element.removeAttribute("rowspan");
				}
			
				if (element.classList.contains("hidden")) {
					element.classList.remove("hidden");
				}
			}));
		},
		checkDeleteData(event) {
			const checkedList = document.querySelectorAll("#contentTable input[name=contentDataCheck]:checked")
			if (checkedList.length <= 0) {
				alert("삭제할 행을 체크해주세요.");
				event.preventDefault();
				return;
			}
			if (!confirm("선택한 항목을 정말 삭제하시겠습니까?")) {
				event.preventDefault();
				return;
			}
			htmx.trigger(_listAreaSelector + " .deleteButton", "deleteModalTrigger");
		},
		checkExportData(event) {
			const checkedList = document.querySelectorAll("#contentTable input[name=contentDataCheck]:checked")
			if (checkedList.length <= 0) {
				alert("export할 행을 체크해주세요");
				event.preventDefault();
				return;
			}
			htmx.trigger(_listAreaSelector + " .exportButton", "exportModalBulkTrigger");
		}
	}
})();


const modalFormFn = (() => {
	const _modalAreaSelector = "#modal";
	
	return {
		// response header로 전달받은 HX-Trigger에 대한 event trigger
		addEventListener() {
			document.addEventListener('showModalFormFragmentResponseTrigger', () => document.querySelector(_modalAreaSelector).showModal());
			document.addEventListener('createModalFormFragmentResponseTrigger', () => this.initialize());
			document.addEventListener('updateModalFormFragmentResponseTrigger', (event) => {
				this.initialize();
				this.copyContentDataToModalForm(event.target.closest("tr").querySelector(".contentData"))
			});
			
			// modalForm에 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 갱신 시 첫 페이지로 이동 처리  
			document.addEventListener('createModalFragmentResponseTrigger', () => param.setParam("page", 1));
			
			document.addEventListener('exportModalBulkFormFragmentResponseTrigger', () => {
				const modalTarget = document.querySelector(_modalAreaSelector);
				const checkedIds = [...document.querySelectorAll("#contentTable input[name=contentDataCheck]")].filter(el => el.checked).map(el => parseInt(el.value));
				const targetList = contentList.filter((_el, index) => checkedIds.includes(index));
				modalTarget.querySelector("textarea").value = JSON.stringify(targetList);
				
				/** (s) 데이터 복사 eventListener */
				modalTarget.querySelector(".copyDataButton").addEventListener("click", () => {
					if (navigator.clipboard == undefined) {
						alert("클립보드 복사가 지원되지 않는 환경입니다.\n직접 복사하여 사용하세요.");
					} else {
						navigator.clipboard.writeText(modalTarget.querySelector("textarea").value)
						.then(() => alert("데이터를 clipboard에 복사하였습니다."))
						.catch(err => console.log('클립보드 복사 실패', err))
					}
				});
				/** (e) 데이터 복사 eventListener */		
			});
			
			// modalForm에 데이터 생성 요청 후 page를 1로 초기화하여 바닥 페이지 데이터 갱신 시 첫 페이지로 이동 처리  
			document.addEventListener('importModalBulkResponseTrigger', () => param.setParam("page", 1));
		},
		/** modalForm에 field type에 따라 input이 생성됨. type에 따라 알맞게 데이터를 설정 */
		// contentDataEl의 hidden input을 그대로 복사하여 설정, 동일 이름이 있으면 값을 추가하도록 처리
		copyContentDataToModalForm(contentDataEl) {
			const modalTarget = document.querySelector(_modalAreaSelector);
			contentDataEl.querySelectorAll("input[type=hidden]").forEach(el => {
				const targetInput = modalTarget.querySelector("[name=" + el.name + "]");
				if (targetInput === null) {
					modalTarget.querySelector("#modalForm").appendChild(el.cloneNode(true));
				} else {
					targetInput.value = el.value;
					// 체크박스는 input과 체크박스 표시가 별도로 존재하여 추가 처리 필요
					const checkBoxInput = targetInput.parentElement.querySelector("input[type=checkbox][name=" + (targetInput.name + "_checkbox") + "]");
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
			const modalTarget = document.querySelector(_modalAreaSelector);
			
			/** (s) 데이터 복사 eventListener */
			// contentTable 영역에 선택한 체크박스 라인의 데이터를 #modalForm의 input 으로 가져온다.
			modalTarget.querySelector(".copyDataButton").addEventListener("click", () => {
				const checkedList = document.querySelectorAll("#contentTable input[name=contentDataCheck]:checked")
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
				event.target.closest('div').querySelector("input[type=hidden][name=" + event.target.name.replace('_checkbox', '') + "]").value = event.target.checked
			));
			/** (e) modal의 checkBox의 on/off 값 설정 eventListener */
			
			/** (s) group 별 input 목록 취합 */
			let columnGroupIdList = [];
			document.querySelectorAll("[data-columngroupid]").forEach(el => columnGroupIdList.push(el.dataset.columngroupid));
			columnGroupIdList = columnGroupIdList.filter((item, pos) => columnGroupIdList.indexOf(item) === pos);
			columnGroupIdList.forEach(columnGroupId => {
				let targetEl;	
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
			let modalBoxRect = modalTarget.querySelector(".modal-box").getBoundingClientRect();
			
			// model 위치
			let modalBoxXStart = modalBoxRect.x;
			let modalBoxXEnd = modalBoxRect.x + modalBoxRect.width;
			let modalBoxYStart = modalBoxRect.y;
			let modalBoxYEnd = modalBoxRect.y + modalBoxRect.height;
			
			modalTarget.querySelectorAll(".modal-box .tooltip").forEach(el => {
				// tooltip class 위치를 확인하여 tooltip의 방향 설정 class 추가
				let targetRect = el.getBoundingClientRect();
				
				let leftGap = targetRect.x - modalBoxXStart;
				let rightGap = modalBoxXEnd - targetRect.x;
				let topGap = targetRect.y - modalBoxYStart;
				let bottomGap = modalBoxYEnd - targetRect.y;
				
				// tooltip의 기본 너비 : 320, 오른쪽 간격이 320 이상이면 오른쪽 배치
				el.classList.add(rightGap > 320 ? "tooltip-right" : leftGap > 320 ? "tooltip-left" : topGap > bottomGap ? "tooltip-top" : "tooltip-bottom");
			
			});
			/** (e) input의 tooltip의 위치 설정 */
		}
	}
})();

listAreaFn.addEventListener();
modalFormFn.addEventListener();


const indicatorFn = (() => {
	let _cloneIndicatorArea = function(target) {
		return document.querySelector(target.getAttribute("hx-indicator")).cloneNode(true);
	}
	
	let _locateIndicatorArea = function(target) {
		let indicator = _cloneIndicatorArea(target);
		indicator.style.display = "block";
		target.innerHTML = "";
		target.appendChild(indicator);
	}
	
	return {
		addEventListener() {
			document.addEventListener('htmx:beforeSwap', (event) => {
				if(event.target.hasAttribute("hx-trigger")) {
					if (event.target.querySelector(event.target.getAttribute("hx-indicator")) != null) {
						return;
					}
					_locateIndicatorArea(event.target);
				}
			});
			
		},
		initialize() {	// 초기 화면 indicator 처리
			document.querySelectorAll("[hx-indicator]").forEach(el => _locateIndicatorArea(el));
		}
	}
})();

indicatorFn.addEventListener();


window.addEventListener('DOMContentLoaded', () => {
	navbarAreaFn.addEventListener();
	
	// 초기화
	searchAreaFn.addEventListener();
	searchAreaFn.initialize();
	
	
	indicatorFn.initialize();
});

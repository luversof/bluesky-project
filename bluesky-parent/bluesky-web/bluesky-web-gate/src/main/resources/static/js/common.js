
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
		deleteParam(paramKey) {
			_params.delete(paramKey);
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

var boardList = (() => {
	return {
		addEventListener() {
			document.addEventListener("listFragmentResponseTrigger", (event) => {
				event.target.querySelectorAll(".navButton").forEach(el => el.addEventListener("click", (event) => {
					param.setParam("page", event.target.dataset.page);
					htmx.trigger("#boardList", "listFragmentTrigger");
				}));
				
				event.target.querySelectorAll("[data-date]").forEach(el => el.textContent = dayjs().to(el.dataset.date));
				
				event.target.querySelectorAll(".writeButton").forEach(el => el.addEventListener("click", () => {
					location.href = "write" + (param.getParams().size > 0 ? "?" + param.getParams().toString() : "");
				}));
				
				event.target.querySelectorAll("table tr[data-boardArticleId]").forEach(el => el.addEventListener("click", (event) => {
					var boardArticleId = event.target.closest("tr").dataset.boardarticleid;
					param.setParam("boardArticleId", boardArticleId)
					location.href = "view" + (param.getParams().size > 0 ? "?" + param.getParams().toString() : "");
				}));
			});
		}
	}	
})();

var boardView = (() => {
	return {
		addEventListener() {
			document.querySelectorAll(".writeButton").forEach(el => el.addEventListener("click", () => {
				location.href = "write" + (param.getParams().size > 0 ? "?" + param.getParams().toString() : "");
			}));
			
			document.querySelectorAll(".listButton").forEach(el => el.addEventListener("click", () => {
				param.deleteParam("boardArticleId");
				location.href = "list" + (param.getParams().size > 0 ? "?" + param.getParams().toString() : "");
			}));
		}
	}
})();

var boardWrite = (() => {
	
})();

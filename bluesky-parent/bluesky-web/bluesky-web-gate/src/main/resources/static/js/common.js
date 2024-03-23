
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


document.addEventListener('htmx:beforeSwap', (event) => {
	if('hx-indicator' in event.target.attributes) {
		var indicator = document.querySelector(event.target.getAttribute('hx-indicator')).cloneNode(true);
		indicator.style.display = "block";
		event.target.innerHTML = "";
	    event.target.appendChild(indicator);
	}
});
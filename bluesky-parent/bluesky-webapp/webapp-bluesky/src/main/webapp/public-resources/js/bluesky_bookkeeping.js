$(document).ready(function() {
	$.bookkeeping = function() {
		var _config = {
			colHeaders : [ "id", "name" ],
			data : null
		};
		var _displayArea = $(".test");
		return {
			init : function() {
				if (_displayArea.data("handsontable") != null) {
					return _displayArea.data("handsontable");
				}
				_displayArea.handsontable({
					colHeaders : [ "id", "name" ],
					data : null,
					afterChange : this.afterChange()
				});
				return _displayArea.data("handsontable");
			},
			add : function(event) {
				var parameter = {
					name : $(".add-bookkeeping input[name=name]").val()
				};

				$.ajax({
					url : "/bookkeeping.json",
					type : "post",
					data : parameter,
					success : function(data) {
						console.log(data);
					}
				});
			},
			load : function(event) {
				$.ajax({
					url : "/bookkeeping.json",
					type : "get",
					success : function(data) {
						event.data.init().loadData(data);
					}
				});
			},
			afterChange : function(change, source) {
				if (source === 'loadData') {
					return; // don't save this change
				}
			}
		};
	}();

	$(".add-bookkeeping").on("click", $.bookkeeping, $.bookkeeping.add);
	$(".load-bookkeeping").on("click", $.bookkeeping, $.bookkeeping.load);
});

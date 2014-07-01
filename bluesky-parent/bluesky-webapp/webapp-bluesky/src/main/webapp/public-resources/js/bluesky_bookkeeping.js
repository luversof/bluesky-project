$(document).ready(function() {
	$.bookkeeping = function() {
		var _displayArea = $(".test");
		return {
			getArea : function() {
				return _displayArea;
			},
			init : function(data) {
				if (_displayArea.data("handsontable") != null) {
					return _displayArea.data("handsontable");
				}
				_displayArea.handsontable({
					colHeaders : [ "id", "name" ],
					columns: [
						{data: 0},
						{data: 1}
					],
					data : data,
					contextMenu: true,
					afterChange : this.afterChange()
				});
				return _displayArea.data("handsontable");
			},
			add : function() {
				var obj = this;
				var parameter = {
					name : $("input[name=name]").val()
				};

				$.ajax({
					url : "/bookkeeping.json",
					type : "post",
					data : parameter,
					success : function(data) {
						obj.getArea().handsontable({data : $.extend(true, {}, data)});
					}
				});
			},
			load : function() {
				var obj = this;
				$.ajax({
					url : "/bookkeeping.json",
					type : "get",
					success : function(data) {
						obj.init(data);
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

	$(".add-bookkeeping").on("click", function() {$.bookkeeping.add();});
	$(".load-bookkeeping").on("click", function() {$.bookkeeping.load();});
});

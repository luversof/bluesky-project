$(document).ready(function() {
	$.bookkeeping = function() {
		var _displayArea = $(".bookkeeping-list");
		return {
			getArea : function() {
				return _displayArea;
			},
			handsontable : function() {
				return _displayArea.data("handsontable");
			},
			init : function() {
				if (this.handsontable() != null) {
					return this.handsontable();
				}
				_displayArea.handsontable({
					rowHeaders: true,
					dataSchema: {id: null, name: null, userId: null},
					colHeaders : [ "name" ],
					colWidths: [300],
					columns: [
						{data: "name"},
					],
					minSpareRows: 1,
					afterChange : this.afterChange
				});
				return this.handsontable();
			},
			load : function() {
				
			},
			/**
			 * 최초 생성시엔 리스트 보여주기가 아닌 해당 가계부로 이동처리 하는게 더 좋지 않을까나?
			 * @returns
			 */
			create : function() {
				var obj = this;
				var parameter = {
					name : $("input[name=name]").val()
				};

				$.ajax({
					url : "/bookkeeping.json",
					type : "post",
					data : parameter,
					success : function(data) {
						var targetData = obj.init().getData();
						targetData.pop();
						targetData.push(data);
						obj.getArea().handsontable({data : targetData});
					}
				});
			},
			load : function() {
				var obj = this;
				$.ajax({
					url : "/bookkeeping.json",
					type : "get",
					success : function(data) {
						obj.init();
						obj.handsontable().loadData(data);
					}
				});
			},
			afterChange : function(change, source, cc) {
				if (source === 'loadData') {
					return; // don't save this change
				}
				//change[0][0] : changed data index
				//change[0][1] : changed data key
				//change[0][2] : changed data orginal value
				//change[0][3] : changed data changed value
				//this.getData()[change[0][0]] : changed data
				targetData = this.getData()[change[0][0]];
				
				$.ajax({
					url: "/bookkeeping.json",
					dataType: "json",
					type: targetData.id == null ? "post" : "put",
					data: targetData, //contains changed cells' data
					success: function (data) {
						console.log('Autosaved (' + change.length + ' ' + 'cell' + (change.length > 1 ? 's' : '') + ')');
					}
				});
			}
		};
	}();

	$(".bookkeeping-create").on("click", function() {$.bookkeeping.create();});
	$(".bookkeeping-load").on("click", function() {
		$.bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-load").trigger("click");
});

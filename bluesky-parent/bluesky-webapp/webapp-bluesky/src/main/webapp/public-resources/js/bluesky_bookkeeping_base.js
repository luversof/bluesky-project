$(document).ready(function() {
	$.Bookkeeping = function(config) {
		// config : {url, displayArea, handsontableConfig}
		var _entryGroupList = null;
		return {
			getArea : function() {
				return config.displayArea;
			},
			handsontable : function() {
				return config.displayArea.data("handsontable");
			},
			init : function() {
				if (this.handsontable() != null) {
					return this.handsontable();
				}
				config.displayArea.handsontable($.extend(config.handsontableConfig, { afterChange : this.afterChange,
					beforeRemoveRow : this.beforeRemoveRow }));
				return this.handsontable();
			},
			load : function() {
				var obj = this;
				$.ajax({
					url : config.url,
					dataType : "json",
					type : "get",
					success : function(data) {
						obj.init();
						obj.handsontable().loadData(data);
					}
				});
			},
			afterChange : function(change, source) {
				if (source === 'loadData') {
					return; // don't save this change
				}
				//change[0][0] : changed data index
				//change[0][1] : changed data key
				//change[0][2] : changed data orginal value
				//change[0][3] : changed data changed value
				//this.getData()[change[0][0]] : changed data
				var targetData = this.getData()[change[0][0]];
				$.ajax({
					url : config.url,
					dataType : "json",
					contentType : "application/json",
					type : targetData.id == null ? "post" : "put",
					data : JSON.stringify(targetData), //contains changed cells' data
					success : function (data) {
					}
				});
			},
			beforeRemoveRow : function(index, amount) {
				var targetData = this.getData()[index];
				if (targetData.id == undefined) {
					return;
				};
				targetData._method = "delete";
				$.ajax({
					url : config.url,
					dataType : "json",
					contentType : "application/json",
					type : "post",
					data : JSON.stringify(targetData), //contains changed cells' data
					success : function (data) {
					}
				});
			},
			/**
			 * entryGroupList를 호출하여 source 형태로 전달? 아니면 변수로 보관?
			 */
			getEntryGroup : function() {
				var entryGroupList = _entryGroupList;
				if (entryGroupList != null) {
					return entryGroupList;
				}
				$.ajax({
					url : "/bookkeeping/entryGroup.json",
					dataType : "json",
					type : "get",
					async : false,
					success : function(data) {
						entryGroupList = data;
					}
				});
				return entryGroupList;
			}
		};
	};
});
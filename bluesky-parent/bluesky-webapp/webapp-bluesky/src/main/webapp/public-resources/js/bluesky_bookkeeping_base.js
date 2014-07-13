$(document).ready(function() {
	$.Bookkeeping = function(config) {
		// config : {url, displayArea, handsontableConfig}
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
				obj.init();
				if (config.initLoad != undefined && config.initLoad == true) {
					obj.handsontable().loadData([{}]);
					return;
				}
				$.ajax({
					url : config.url,
					dataType : "json",
					type : "get",
					success : function(data) {
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
				$.ajax({
					url : config.url,
					dataType : "json",
					contentType : "application/json",
					type : "delete",
					data : JSON.stringify(targetData), //contains changed cells' data
					success : function (data) {
					}
				});
			},
		};
	};
});
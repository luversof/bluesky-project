$(document).ready(function() {
	$.Bookkeeping = function(config) {
		var _config = config;
		// config : {url, displayArea, handsontableConfig}
		var _handsontable = function() {
			return _config.displayArea.data("handsontable");
		}
		return {
			init : function() {
				if (_handsontable() != null) {
					return _handsontable();
				}
				_config.displayArea.handsontable($.extend(_config.handsontableConfig, { afterChange : this.afterChange, beforeRemoveRow : this.beforeRemoveRow }));
				return _handsontable();
			},
			load : function() {
				this.init();
				if (_config.initLoad != undefined && _config.initLoad == true) {
					_handsontable().loadData([{}]);
					return;
				}
				$.ajax({
					url : _config.postUrl.format(_config.bookkeepingId),
					dataType : "json",
					type : "get"
				}).success(function(data) {
					_handsontable().loadData(data);
					_config.displayArea.data("dataObj", data);
				});
			},
			afterChange : function(change, source) {
				console.log("source : ", source);
				if (source === 'loadData') {
					return; // don't save this change
				}
				//change[0][0] : changed data index
				//change[0][1] : changed data key
				//change[0][2] : changed data orginal value
				//change[0][3] : changed data changed value
				//this.getData()[change[0][0]] : changed data
				console.log("this.getData() : ", this.getData());
				console.log("change : ", change);
				console.log("config : ", _config);
				var targetCellData = this.getData()[change[0][0]];
				console.log("targetCellData :", targetCellData);
				
				if (targetCellData == undefined) {
					return;
				}
				
				// 신규 추가인 경우 모든 cell이 입력된 이후 추가 처리를 함 (TODO 이거 나중에 필수가 아닌 cell 입력에 대해서는 예외처리 추가 필요함)
				for (var i = 1; i < targetCellData.length; i++) {
					if (targetCellData[i] == null) {
						return;
					}
				}
				
				var dataObj = _config.displayArea.data("dataObj");
				console.log("dataObj : ", dataObj);
				
				console.log("a : ", $.isArray(dataObj));
				
				var targetData;
				var targetIndex = -1;
				
				if ($.isArray(dataObj)) {
					for (var i = 0 ; i < dataObj.length ; i++) {
						if (dataObj[i].id == targetCellData[0]) {
							targetData = dataObj[i];
							targetIndex = i;
							break;
						}
					}
					console.log("targetData : ", targetData);
					
					// 신규 추가의 경우 마지막에서 -1의 data가 해당 데이터임
					if (targetData.id == null) {
						targetData = dataObj[change[0][0]];
						console.log("dataObj[change[0][0]] :", dataObj[dataObj.length -2]);
					}
				} else {// 단일 object인 경우는 수정만 가능한 경우
					targetData = dataObj;
				}
				
				targetData[change[0][1]] = change[0][3];
				
				//targetData[change[0][1]] = change[0][3];
				console.log("targetData : ", targetData);
				console.log("test : ", this);
				
				
				$.ajax({
					url : targetData.id == null ? _config.postUrl.format(_config.bookkeepingId) : _config.putUrl.format(_config.bookkeepingId, targetData.id),
					dataType : "json",
					contentType : "application/json",
					type : targetData.id == null ? "post" : "put",
					data : JSON.stringify(targetData)
				}).success(function (data) {
					console.log(targetData);
					if (targetIndex == -1) {
						dataObj = data;
					} else {
						dataObj[targetIndex] = data;
					}
					console.log("dataObj : ", dataObj);
					_handsontable().loadData(dataObj);
				});
			},
			beforeRemoveRow : function(index, amount) {
				var targetCellData = this.getData()[index];
				if (targetCellData[0] == undefined) {
					return;
				};
				
				var dataObj = _config.displayArea.data("dataObj");
				
				var targetData;
				for (var i = 0 ; i < dataObj.length ; i++) {
					if (dataObj[i].id == targetCellData[0]) {
						targetData = dataObj[i];
					}
				}
				
				$.ajax({
					url : _config.putUrl.format(_config.bookkeepingId, targetData.id),
					dataType : "json",
					contentType : "application/json",
					type : "delete",
					data : JSON.stringify(targetData),
					success : function (data) {
					}
				});
			}
		};
	};
});
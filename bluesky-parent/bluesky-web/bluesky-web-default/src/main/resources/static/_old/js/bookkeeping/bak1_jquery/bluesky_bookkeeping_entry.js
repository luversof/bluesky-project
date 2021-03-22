$(document).ready(function() {
	/**
	 * entryGroupList를 호출하여 source 형태로 전달? 아니면 변수로 보관?
	 */
	$.entryGroup = function() {
		var _entryGroupList = null;
		return {
			getEntryGroupList : function() {
				if (_entryGroupList != null) {
					return _entryGroupList;
				}
				$.ajax({
					url : "/bookkeeping/{0}/entryGroup".format(bookkeepingId),
					dataType : "json",
					type : "get",
					async : false,
					success : function(data) {
						_entryGroupList = data;
					}
				});
				return _entryGroupList;	
			},
			getDebitEntryGroupSource : function() {
				this.getEntryGroupList();
				var debitEntryGroupSource = null || [];
				for (var i = 0 ; i < _entryGroupList.length ; i++) {
					debitEntryGroupSource.push(_entryGroupList[i]["name"]);
					//console.log(_entryGroupList[i]);
				}
				return debitEntryGroupSource;
			}
		};
	}();
	
	var bookkeepingDebit = $.Bookkeeping({
		postUrl : "/bookkeeping/{0}/entry",
		putUrl : "/bookkeeping/{0}/entry/{1}",
		displayArea : $(".bookkeeping-entry-debit-form"),
		initLoad : true,
		handsontableConfig : {
				contextMenu : [ "remove_row" ],
				dataSchema : {
					id : null
					, entryGroup : { name : null }
					, amount : null, memo : null
				},
				colHeaders : [ "id", "날짜", "분류", "자산", "금액", "메모" ],
				colWidths : [ 30, 120, 120, 80, 180 ],
				columnSorting : true,
				columns : [
					{ data : "id", readOnly : true },
					{ data : "entryDate", type : "date", dateFormat: 'YYYY-MM-DD' },
					{ data : "entryGroup",
						type : "handsontable",
						handsontable : {
							data : $.entryGroup.getEntryGroupList(),
							//colHeaders : [ "id", "name" ],
							colWidths : [ 30, 100 ],
							columns : [
								{ data : "id" },
								{ data : "name" }
							]
						} 
					},
					{ data : "debitAsset.name" },
					{ data : "amount" , type : "numeric", format : "0,0" },
					{ data : "memo" }
				]
		}
	});

	bookkeepingDebit.load();

//	$(".bookkeeping-entry-load").on("click", function() {
//		$(this).hide();
//	});
//	$(".bookkeeping-entry-load").trigger("click");
	
	var a =	{
		"id":1,
		"bookkeeping":{
			"id":50,
			"name":"테스트",
			"userId":22
		},
		"debitAsset":{
			"id":9,
			"name":"1ㄴㅁㅇㅎㄴㅁㅇㅎ",
			"amount":0,
			"assetType":"WALLET"
		},
		"creditAsset":{
			"id":2,
			"name":"상여금",
			"amount":0,
			"assetType":"WALLET"
		},
		"entryGroup":{
			"id":19,
			"name":"교육",
			"entryType":"DEBIT"
		},
		"amount":123,
		"entryDate":"2014-07-02",
		"memo":"testmemo"
	};
});

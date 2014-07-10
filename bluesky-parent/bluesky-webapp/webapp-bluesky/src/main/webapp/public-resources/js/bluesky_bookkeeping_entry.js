$(document).ready(function() {
	var bookkeeping = $.Bookkeeping({
		url : "/bookkeeping/entry.json",
		displayArea : $(".bookkeeping-entry-debit-form"),
		initLoad : true,
		handsontableConfig : {
				rowHeaders : true,
				contextMenu : [ "remove_row" ],
//				dataSchema: {
//					"id" : null,
//					,"debitAsset":{"id":9,"name":"1","amount":0,"assetType":"WALLET"},"creditAsset":{"id":2,"name":"상여금","amount":0,"assetType":"WALLET"},"entryGroup":{"id":19,"name":"교육","entryType":"DEBIT"},"amount":21123,"entryDate":"2014-07-03","memo":"testmemo"},
				colHeaders : [ "날짜", "금액", "분류", "메모" ],
				colWidths : [ 120, 120, 80, 180 ],
				columnSorting : true,
				columns : [
					{ data : "entryDate" },
					{ data : "amount" , type : "numeric", format : "0,0" },
					{ data : "entryGroup.name" },
					{ data : "memo" }
				],
		}
	});

	bookkeeping.load();

//	$(".bookkeeping-entry-load").on("click", function() {
//		$(this).hide();
//	});
//	$(".bookkeeping-entry-load").trigger("click");
});

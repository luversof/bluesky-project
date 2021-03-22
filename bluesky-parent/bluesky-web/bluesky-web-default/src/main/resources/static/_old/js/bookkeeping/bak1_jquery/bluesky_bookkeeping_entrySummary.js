$(document).ready(function() {
	var bookkeeping = $.Bookkeeping({
		url : "/bookkeeping/entry.json",
		displayArea : $(".bookkeeping-entry-list"),
		handsontableConfig : {
				rowHeaders : true,
				contextMenu : [ "remove_row" ],
//				dataSchema : {id: null, name: null, userId: null},
				colHeaders : [ "날짜", "금액", "분류", "메모" ],
				colWidths : [ 120, 120, 80, 180 ],
				columnSorting : true,
				columns : [
					{ data : "entryDate", type : "date" },
					{ data : "amount" , type : "numeric", format : "0,0" },
					{ data : "entryGroup.name", readOnly : true },
					{ data : "memo" }
				]
		}
	});

	$(".bookkeeping-entry-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-entry-load").trigger("click");
});

$(document).ready(function() {
	var bookkeeping = $.Bookkeeping({
		url : "/bookkeeping/entry.json",
		displayArea : $(".bookkeeping-entry-list"),
		handsontableConfig : {
				rowHeaders: true,
				contextMenu: [ "remove_row" ],
//				dataSchema: {id: null, name: null, userId: null},
				colHeaders : [ "entryDate", "amount" ],
				colWidths: [300, 180],
				columnSorting: true,
				columns: [
					{ data : "entryDate",  type : "date", format : "yyyy/mm/dd"},
					{ data : "amount" }
				],
				minSpareRows: 1,
		}
	});

	$(".bookkeeping-entry-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-entry-load").trigger("click");
});

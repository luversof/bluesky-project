$(document).ready(function() {
	var bookkeeping = $.Bookkeeping({
		url : "/bookkeeping/" + bookkeepingId + "/bookkeeping",
		displayArea : $(".bookkeeping-list"),
		handsontableConfig : {
				rowHeaders : true,
				contextMenu : [ "remove_row" ],
				dataSchema : {id: null, name: null, userId: null},
				colHeaders : [ "name" ],
				colWidths : [300],
				columnSorting : true,
				columns : [
					{ data : "name" }
				],
				minSpareRows : 1,
		}
	});

	$(".bookkeeping-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-load").trigger("click");
});

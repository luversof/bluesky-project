$(document).ready(function() {
	var bookkeeping = $.Bookkeeping({
		bookkeepingId : bookkeepingId,
		postUrl : "/bookkeeping/{0}",
		putUrl : "/bookkeeping/{1}",
		displayArea : $(".bookkeeping-list"),
		handsontableConfig : {
				allowInsertRow : false,
				//contextMenu : [ "remove_row" ],
				dataSchema : {id: null, name: null, userId: null},
				colHeaders : [ "id", "name" ],
				colWidths : [50, 300],
				columnSorting : true,
				columns : [
					{ data : "id", readOnly : true },
					{ data : "name" }
				],
				minSpareRows : 1
		}
	});

	$(".bookkeeping-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-load").trigger("click");
});

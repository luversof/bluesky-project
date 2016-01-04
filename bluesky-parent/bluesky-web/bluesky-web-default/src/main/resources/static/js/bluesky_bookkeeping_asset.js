$(document).ready(function() {
	var displayArea = $(".bookkeeping-asset-list");
	var bookkeeping = $.Bookkeeping({
		bookkeepingId : bookkeepingId,
		postUrl : "/bookkeeping/{0}/asset",
		putUrl : "/bookkeeping/{0}/asset/{1}",
		displayArea : displayArea,
		handsontableConfig : {
				contextMenu : [ "remove_row" ],
				dataSchema : { "id" : null, "assetType" : null, "name" : null, "amount" : 0 },
				colHeaders : [ "id", "assetType", "name", "amount" ],
				colWidths : [30, 100, 200, 80],
				columnSorting : true,
				columns : [
					{ data : "id", readOnly : true },
					{ data : "assetType", editor : "select", selectOptions : assetTypeList },
					{ data : "name" },
					{ data : "amount", readOnly : true, type : "numeric" }
				],
				minSpareRows : 1,
		}
	});

	$(".bookkeeping-asset-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-asset-load").trigger("click");
});

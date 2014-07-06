$(document).ready(function() {
	var bookkeeping = $.Bookkeeping({
		url : "/bookkeeping/asset.json",
		displayArea : $(".bookkeeping-asset-list"),
		handsontableConfig : {
				rowHeaders: true,
				contextMenu: [ "remove_row" ],
				dataSchema: { "id" : null, "name" : null, "amount" : null, "assetType" : null},
				colHeaders : [ "assetType", "name", "amount" ],
				colWidths: [100, 200, 80],
				columnSorting: true,
				columns: [
					{ data: "assetType", type: 'dropdown', source : assetTypeList },
					{ data: "name" },
					{ data: "amount", readOnly : true }
				],
				minSpareRows: 1,
		}
	});

	$(".bookkeeping-asset-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-asset-load").trigger("click");
});

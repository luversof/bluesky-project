$(document).ready(function() {
	var bookkeeping = $.Bookkeeping({
		url : "/bookkeeping/entryGroup.json",
		displayArea : $(".bookkeeping-entryGroup-list"),
		handsontableConfig : {
				rowHeaders: true,
				contextMenu: [ "remove_row" ],
				dataSchema: {
					"id": null,
					"name": null,
					"entryType": null
				},
				colHeaders : [ "entryType", "name" ],
				colWidths: [100, 300],
				columnSorting: true,
				columns: [
				    { data : "entryType", readOnly : true, type : 'dropdown', source : entryTypeList },
					{ data : "name" }
				],
				cells: function (row, col, prop) {
				      var cellProperties = {};
				      if ($(".bookkeeping-entryGroup-list").handsontable('getData')[row][prop] == null && col == 0) {
				    	  cellProperties.readOnly = false;  
				      }
				      return cellProperties;
				},
				minSpareRows: 1,
		}
	});

	$(".bookkeeping-entryGroup-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-entryGroup-load").trigger("click");
});

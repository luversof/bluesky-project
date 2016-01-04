$(document).ready(function() {
	var displayArea = $(".bookkeeping-entryGroup-list");
	var bookkeeping = $.Bookkeeping({
		bookkeepingId : bookkeepingId,
		postUrl : "/bookkeeping/{0}/entryGroup",
		putUrl : "/bookkeeping/{0}/entryGroup/{1}",
		displayArea : displayArea,
		handsontableConfig : {
				//rowHeaders : true,
				contextMenu : [ "remove_row" ],
				dataSchema : {
					"id": null,
					"entryType": null,
					"name": null
				},
				colHeaders : [ "id", "entryType", "name" ],
				colWidths : [30, 100, 300],
				columnSorting : true,
				columns : [
				    { data : "id", readOnly : true },
				    { data : "entryType", readOnly : true, editor : "select", selectOptions : entryTypeList },
					{ data : "name" }
				],
				cells : function (row, col, prop) {
				      var cellProperties = {};
				      if (displayArea.handsontable('getData')[row][col] == null && col == 1) {
				    	  cellProperties.readOnly = false;  
				      }
				      return cellProperties;
				},
				minSpareRows : 1,
		}
	});

	$(".bookkeeping-entryGroup-load").on("click", function() {
		bookkeeping.load();
		$(this).hide();
	});
	$(".bookkeeping-entryGroup-load").trigger("click");
});

$.StatisticsCollection = Backbone.Collection.extend({
	model : $.Statistics,
	url : "/bookkeeping/" + $.bookkeepingId + "/statistics",
	sortColumn : "entryType",
	sortDirection : "asc",
//	comparator: "entryDate",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
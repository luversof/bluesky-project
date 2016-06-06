$.StatisticsCollection = Backbone.Collection.extend({
	model : $.Statistics,
	url : "/bookkeeping/" + $.bookkeepingId + "/statistics",
//	comparator: "entryDate",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
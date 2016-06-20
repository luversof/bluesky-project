$.AssetCollection = Backbone.Collection.extend({
	model : $.Asset,
	url : "/bookkeeping/" + $.bookkeepingId + "/asset",
	sortColumn : "assetType",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
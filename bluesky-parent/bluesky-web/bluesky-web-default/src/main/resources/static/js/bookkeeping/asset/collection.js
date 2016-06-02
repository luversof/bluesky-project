$.AssetCollection = Backbone.Collection.extend({
	model : $.Asset,
	url : "/bookkeeping/" + $.bookkeepingId + "/asset",
//	comparator: "assetType",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
$.AssetCollection = Backbone.Collection.extend({
	model : $.Asset,
	url : "/bookkeeping/" + $.bookkeepingId + "/asset",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
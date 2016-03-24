$.AssetCollection = Backbone.Collection.extend({
	model : $.Asset,
	url : location.pathname.replace("/setting/asset", "") + "/asset",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
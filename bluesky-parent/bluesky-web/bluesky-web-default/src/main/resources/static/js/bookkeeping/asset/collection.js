$.AssetCollection = Backbone.Collection.extend({
	model : $.Asset,
	url : location.pathname.replace("/asset/setting", "") + "/asset",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
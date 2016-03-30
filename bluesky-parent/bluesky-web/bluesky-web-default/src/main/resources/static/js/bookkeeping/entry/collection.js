$.EntryCollection = Backbone.Collection.extend({
	model : $.Entry,
	url : location.pathname.replace("/entry/index", "") + "/entry",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
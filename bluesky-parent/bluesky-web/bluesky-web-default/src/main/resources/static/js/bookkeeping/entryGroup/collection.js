$.EntryGroupCollection = Backbone.Collection.extend({
	model : $.EntryGroup,
	url : location.pathname.replace("/entryGroup/setting", "") + "/entryGroup",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
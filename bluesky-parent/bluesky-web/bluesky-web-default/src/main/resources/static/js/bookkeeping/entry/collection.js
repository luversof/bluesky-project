$.EntryCollection = Backbone.Collection.extend({
	model : $.Entry,
	url : "/bookkeeping/" + $.bookkeepingId + "/entry",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
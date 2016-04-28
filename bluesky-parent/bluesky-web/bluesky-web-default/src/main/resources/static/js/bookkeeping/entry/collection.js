$.EntryCollection = Backbone.Collection.extend({
	model : $.Entry,
	url : "/bookkeeping/" + $.bookkeepingId + "/entry",
//	comparator: "entryDate",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
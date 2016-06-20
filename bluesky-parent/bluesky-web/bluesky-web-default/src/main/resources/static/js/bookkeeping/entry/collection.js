$.EntryCollection = Backbone.Collection.extend({
	model : $.Entry,
	url : "/bookkeeping/" + $.bookkeepingId + "/entry",
	sortColumn : "entryDate",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
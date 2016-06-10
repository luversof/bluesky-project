$.EntryCollection = Backbone.Collection.extend({
	model : $.Entry,
	url : "/bookkeeping/" + $.bookkeepingId + "/entry",
	sortColumn : "entryDate",
	sortDirection : "asc",
//	comparator: "entryDate",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
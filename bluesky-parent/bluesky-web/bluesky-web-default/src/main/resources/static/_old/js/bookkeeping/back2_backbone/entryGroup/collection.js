$.EntryGroupCollection = Backbone.Collection.extend({
	model : $.EntryGroup,
	bookkeepingId : 0,
	url : "/bookkeeping/" + $.bookkeepingId + "/entryGroup",
	sortColumn : "entryType",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
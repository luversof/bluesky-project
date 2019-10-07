$.BookkeepingCollection = Backbone.Collection.extend({
	model : $.Bookkeeping,
	url : "/bookkeeping",
	initialize : function() {
		//console.log("BookkeepingCollection initialized.");
	}
});
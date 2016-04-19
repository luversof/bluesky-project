$.EntrySearchInfo = Backbone.Model.extend({
	url : "/bookkeeping/" + $.bookkeepingId + "/entrySearchInfo",
	defaults : {
		bookkeepingId : $.bookkeepingId,
		startDateTime : null,
		endDateTime : null
	},
	initialize : function() {
		//console.log("This model has been initialized.");
		this.on("change", function() {
			//console.log("this model has changed.");
		});
		this.on("change:name", function() {
			//console.log("name value for this model has changed.");
		});
		this.on("invalid", function(model, error) {
			alert(error);
		});
	},
	validate : function(attrs, options) {
		//console.log("validate : ", attrs, options);
		if (attrs.bookkeepingId == 0) {
			return "가계부의 Id를 입력하세요";
		}
	}
});
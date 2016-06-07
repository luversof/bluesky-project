$.EntrySearchInfo = Backbone.Model.extend({
	url : "/bookkeeping/" + $.bookkeepingId + "/entrySearchInfo",
	defaults : {
		bookkeeping : { id : $.bookkeepingId },
		targetLocalDate : null,
		getMomentDateFormat : function() {
			return "YYYY-MM";
		},
		getMomentManipulateKey : function() {
			return "months";
		},
		getDatepickerDateFormat : function() {
			return this.getMomentDateFormat().toLowerCase();
		}
	},
	initialize : function() {
		this.on("invalid", function(model, error) {
			alert(error);
		});
	},
	validate : function(attrs, options) {
		//console.log("validate : ", attrs, options);
		if (attrs.bookkeeping.id == 0) {
			return "가계부의 Id를 입력하세요";
		}
	}
});
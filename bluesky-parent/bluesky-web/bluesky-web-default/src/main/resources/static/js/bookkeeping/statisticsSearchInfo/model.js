$.StatisticsSearchInfo = Backbone.Model.extend({
	url : "/bookkeeping/" + $.bookkeepingId + "/statisticsSearchInfo",
	defaults : {
		bookkeeping : { id : $.bookkeepingId },
		chronoUnit : "YEARS",
		targetLocalDate : null,
		getMomentDateFormat : function() {
			if (this.chronoUnit == "YEARS") {
				return "YYYY";
			} else if (this.chronoUnit == "MONTHS") {
				return "YYYY-MM";
			}
			return "";
		},
		getMomentManipulateKey : function() {
			return this.chronoUnit.toLowerCase();
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
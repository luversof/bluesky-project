$.Statistics = Backbone.Model.extend({
	defaults : {
		entryGroup : null,
		amount : 0,
	},
	initialize : function() {
		this.on("invalid", function(model, error) {
			alert(error);
		});
	},
	validate : function(attrs, options) {
		console.log("validate : ", attrs, options);
		if (attrs.amount == 0) {
			return "금액을 입력하세요.";
		}
	}
});
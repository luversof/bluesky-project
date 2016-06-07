$.EntryGroup = Backbone.Model.extend({
	defaults : {
		bookkeeping : { id : $.bookkeepingId },
		name : null,
		entryType : null
	},
	initialize : function() {
		this.on("invalid", function(model, error) {
			alert(error);
		});
	},
	validate : function(attrs, options) {
		console.log("validate : ", attrs, options);
		if (attrs.name == "") {
			return "추가할 자산의 이름을 입력하세요.";
		}
		if (attrs.entryType == null) {
			return "test";
		}
	}
});
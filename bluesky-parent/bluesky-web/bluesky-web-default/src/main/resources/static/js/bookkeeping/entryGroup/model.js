$.EntryGroup = Backbone.Model.extend({
	defaults : {
		name : null,
		entryType : null
	},
	initialize : function() {
		//console.log("This model has been initialized.");
//		this.on("change", function() {
//			//console.log("this model has changed.");
//		});
//		this.on("change:name", function() {
//			//console.log("name value for this model has changed.");
//		});
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
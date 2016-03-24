$.Asset = Backbone.Model.extend({
	defaults : {
		name : null,
		amount : 0,
		assetType : null
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
			//console.log("invalid : ", model, error);
			alert(error);
		})
	},
	validate : function(attrs, options) {
		console.log("validate : ", attrs, options);
		if (attrs.name == "") {
			return "추가할 자산의 이름을 입력하세요.";
		}
		if (attrs.assetType == null) {
			return "test";
		}
	}
});
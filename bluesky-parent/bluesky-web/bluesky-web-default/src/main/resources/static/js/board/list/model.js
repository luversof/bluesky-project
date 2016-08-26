$.BbsArticle = Backbone.Model.extend({
	defaults : {
		bookkeeping : { id : $.bookkeepingId },
		name : null,
		amount : 0,
		assetType : null
	},
	initialize : function() {
		this.on("invalid", function(model, error) {
			//alert(error);
		});
	},
	validate : function(attrs, options) {
		//console.log("validate : ", attrs, options);
		if (attrs.name === "") {
			return "추가할 자산의 이름을 입력하세요.";
		}
		if (attrs.assetType === null) {
			return "test";
		}
	}
});
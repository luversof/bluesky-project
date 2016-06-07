$.Bookkeeping = Backbone.Model.extend({
	defaults : {
		name : null,
		userId : 0,
		baseDate : 1
	},
	initialize : function() {
		this.on("invalid", function(model, error) {
			alert(error);
		});
	},
	validate : function(attrs, options) {
		//console.log("validate : ", attrs, options);
		if (attrs.name == "") {
			return "가계부의 이름을 입력하세요";
		}
		if (attrs.baseDate <= 0) {
			return "기준일을 입력하세요";
		}
	}
});
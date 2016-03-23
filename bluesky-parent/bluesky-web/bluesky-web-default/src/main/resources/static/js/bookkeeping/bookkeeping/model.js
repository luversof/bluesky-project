$.Bookkeeping = Backbone.Model.extend({
	defaults : {
		name : null,
		userId : 0
	},
	initialize : function() {
		console.log("This model has been initialized.");
		this.on("change", function() {
			console.log("this model has changed.");
		});
		this.on("change:name", function() {
			console.log("name value for this model has changed.");
		});
		this.on("invalid", function(model, error) {
			console.log("invalid : ", model, error);
		})
	},
	validate : function(attrs, options) {
		console.log("validate : ", attrs, options);
		if (attrs.name === undefined) {
			return "Remember to set a name for your bookkeeping";
		}
	}
});
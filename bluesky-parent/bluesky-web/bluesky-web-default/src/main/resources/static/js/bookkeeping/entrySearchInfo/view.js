$(document).ready(function() {
	
	$.EntrySearchInfoView = Backbone.View.extend({
		el : "[data-menu-area=entrysearchInfo]",
		template : $("#template-entrySearchInfo-view").html(),
		render : function() {
			console.log("entrySearchInfoView render");
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			return this;
		}
	});
});
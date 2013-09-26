// 템플릿에 아이템을 추가, 삭제, 보여주기

// List - Item
// Data, Template


var Entry = Backbone.Model.extend({
	urlRoot : "/user/1/bookkeeping/entry"
});

var EntryList = Backbone.Collection.extend({
	url : "/user/1/bookkeeping/entry",
	model : Entry
});

var EntryView = Backbone.View.extend({
	tagName : "tr",
	initialize : function() {
		if (this.options.template == null) {
			console.log("template argument is required");
		}
	},
	render : function() {
		this.$el.append(Mustache.render(this.options.template, this.model.toJSON()));
		return this;
	},
	events : {
		"mouseenter" : "menuShow",
		"mouseleave" : "menuHide",
		"click span.glyphicon-edit" : "modify",
		"keyup [contenteditable=true]" : "change"
	},
	menuShow : function(event) {
		$(event.currentTarget).find("td:last").empty().append(
				$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip()
			).append(" ").append(
				$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip()
			).append(" ").append(
				$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().hide()
			);
	},
	menuHide : function(event) {
		$(event.currentTarget).find("td:last").empty();
	},
	modify : function(event) {
		console.log(this.model);
		if (this.model.hasChanged()) {
			this.model.save();
		}
	},
	change : function(event) {
		this.model.set($(event.currentTarget).attr("data-key"), $(event.currentTarget).text());
	}
});

var EntryListView = Backbone.View.extend({
	initialize : function() {
		if (this.options.template == null) {
			console.log("template argument is required");
		}
	},
	render: function() {
		this.collection.each(this.addOne, this);
		return this;
	},
	addOne : function(entry) {
		var entryView = new EntryView({ model: entry, template : this.options.template });
		this.$el.append(entryView.render().el);
	}
});


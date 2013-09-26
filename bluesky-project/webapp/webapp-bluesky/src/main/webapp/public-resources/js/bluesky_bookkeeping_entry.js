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

var EntryListView = Backbone.View.extend({
	initialize : function() {
		console.log("EntryView initialize");
		this.template = $("#entry-template").text();
		this.initListView();
	},
	
	initListView : function() {
		this.$el.empty();
		for (var target in this.collection.toJSON()) {
			this.$el.append(Mustache.render(this.template, { entryList : target }));
		}
		return this;
	},
	
	render : function() {
		this.$el.html(Mustache.render(this.template, { entryList : this.model.toJSON() }));
		return this;
	},
	events : {
		"mouseenter tr" : "menuShow"
	},
	menuShow : function(event) {
		console.log("TEstsestetet");
		var b = "";
		for (var a in this.model) b+=a+"\n";
		console.log("b : " + b);
		console.log("model : " + this.model);
	}
});


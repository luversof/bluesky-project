// 템플릿에 아이템을 추가, 삭제, 보여주기

// List - Item
// Data, Template

window.Entry = Backbone.Model.extend({
	urlRoot : "/user/1/bookkeeping/entry"
});

var EntryList = Backbone.Collection.extend({
	url : "/user/1/bookkeeping/entry",
	model : Entry
});

var EntryView = Backbone.View.extend({
	initialize : function() {
		console.log("EntryView initialize");
	}
});
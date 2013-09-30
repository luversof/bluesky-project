// 템플릿에 아이템을 추가, 삭제, 보여주기

function showMessageModal(message) {
	return $("<div>").addClass("modal fade").html(
		$("<div>").addClass("modal-dialog").html(
			$("<div>").addClass("modal-content").html(
				$("<div>").addClass("modal-body").text(message)
			)
		)
	).modal();
};

var Entry = Backbone.Model.extend({
	urlRoot : "/user/1/bookkeeping/entry",
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
		//this.listenTo(this.model, "add", this.render);
		this.listenTo(this.model, "sync", this.render);
	},
	render : function() {
		console.log("render");
		this.$el.empty().append(Mustache.render(this.options.template, this.model.toJSON()));
		return this;
	},
	events : {
		"mouseenter" : "menuShow",
		"mouseleave" : "menuHide",
		"click span.glyphicon-edit" : "modify",
		"click span.glyphicon-remove" : "removeEntry",
		"click span.glyphicon-refresh" : "reset",
		"keyup [contenteditable=true]" : "dataChangeListener"
	},
	menuShow : function(event) {
		$(event.currentTarget).find("td:last").empty().append(
				$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip()
			).append(" ").append(
				$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip()
			).append(" ").append(
				$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().hide()
			);
		this.menuResetDisplayCheck($(event.currentTarget).find(".glyphicon-refresh"));
	},
	menuResetDisplayCheck : function(displayTarget) {
		displayTarget.hide();
		if (!this.model.hasChanged()) {
			return;
		}
		for (var key in this.model.changedAttributes()) {
			if (this.model.get(key) != this.model.previous(key)) {
				displayTarget.fadeIn();	
			}
		}
	},
	menuHide : function(event) {
		$(event.currentTarget).find("td:last").empty();
	},
	modify : function(event) {
		console.log(this.model);
		if (this.model.hasChanged()) {
			this.model.save(null, {
				success : function() {
					showMessageModal("asset changed");
				}
			});
		}
	},
	removeEntry : function(event) {
		this.remove();
		this.model.destroy({
			success : function() {
				showMessageModal("asset removed");
			}
		});
	},
	reset : function(event) {
		console.log("reset");
		var target = this;
		this.model.fetch({ success : function(response, xhr) { 
			target.render();
			target.model.changed = {};	//backbonejs 매뉴얼에 하지말라는 부분인데 다른 방법이 없나..
		}});
	},
	dataChangeListener : function(event) {
		this.model.set($(event.currentTarget).attr("data-key"), $(event.currentTarget).text());
		this.menuResetDisplayCheck($(event.currentTarget).parent(this.tagName).find(".glyphicon-refresh"));
	}	
});

var EntryListView = Backbone.View.extend({
	el : ".table.table-hover tbody",
	initialize : function() {
		if (this.options.template == null) {
			console.log("template argument is required");
		}
		this.$el.empty();
		//this.listenTo(this.collection, "add", this.add);
		//this.listenTo(this.collection, "add", this.addOne);
		console.log(this);
		this.listenTo(this.collection, "sync", this.render);
	},
	render: function() {
		console.log("entryListView render");
		this.collection.each(this.addOne, this);
		return this;
	},
	addOne : function(entry) {
		var entryView = new EntryView({ model: entry, template : this.options.template});
		this.$el.append(entryView.render().el);
	},
	add : function(entry) {
		entry.save(null, { success : function() {
			$(".entry-add-modal").modal("hide");
		}});
	}
});

var entryPage = function(config) {
	this.entryList = config.entryList;
	this.entryListView = config.entryListView;
	return {
		initialize : function() {
			this.eventShowMenuAdd();
			this.eventActionMenuAdd();
		},
		eventShowMenuAdd : function() {
			$(".entry-menu-add").css("cursor", "pointer").on("click", function(event) {
				console.log("eventMenuAdd");
				$(".entry-add-modal").modal();
			});
		},
		eventActionMenuAdd : function() {
			$(".entry-add").on("click",function() {
				console.log("entry-add");
				var entry = new Entry({
					asset : { id : $("[id='asset.id']").val()},
					entryGroup : {id : $("[id='entryGroup.id']").val()},
					amount : $("#amount").val(),
					date : $("#date").val(),
					memo : $("#memo").val(),
					transferEntry : $("#transferEntry").is(":checked")
				});
//				entryListView.collection.add(entry);
				entry.save(null ,{success : function() {
					console.log("ETstt");
				}});
			});
		}
	}
};



$(document).ready(function() {
	//entry.start("[[@{/}]]", [[${#authentication.principal.id}]]);
	//이거 안해도 되게 안되나?
	$("[data-toggle=tooltip]").tooltip();
	
	/* (s) 저장 */
	/* var entry = new Entry({asset : {id : 1}, entryGroup : { id : 1}, amount : 123, memo : "test"});
	console.log("entry.url() : " + entry.url());
	entry.save(); */
	/* (e) 저장 */
	
	/* (s) 호출 */
	/* var entry = new Entry({id : 1});
	console.log("entry.url() : " + entry.url());
	entry.fetch({
		success : function(response, xhr) {
			console.log("fetch success");
			console.log(response);
			console.log("ge1111t : " + entry.get("memo"));
		}
	}); */
	/* (e) 호출 */
	
	var entryList = new EntryList();
	var entryListView = new EntryListView({ collection : entryList ,template : $("#entry-template").text() });
	
	entryList.fetch();
	entryPage({entryList : entryList, entryListView : entryListView}).initialize();
	setInterval("console.log(entryListView.collection.length);", 1000);
});
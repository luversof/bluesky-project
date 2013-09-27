// 템플릿에 아이템을 추가, 삭제, 보여주기

// List - Item
// Data, Template
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
		//this.listenTo(this.model, "change", this.render);
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
//		if (this.model.changedAttributes()) {
//			$(event.currentTarget).find(".glyphicon-refresh").fadeIn();
//		} else {
//			$(event.currentTarget).find(".glyphicon-refresh").hide();
//		}
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
	reset : function(event) {
		console.log("reset");
		var target = this;
		this.model.fetch({ success : function(response, xhr) { 
			target.render();
			target.model.changed = {};
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
		console.log(this);
		this.eventMenuAdd();
	},
	render: function() {
		this.collection.each(this.addOne, this);
		return this;
	},
	addOne : function(entry) {
		var entryView = new EntryView({ model: entry, template : this.options.template});
		this.$el.append(entryView.render().el);
	},
	eventMenuAdd : function() {
		$(".entry-menu-add").css("cursor", "pointer").on("click", function(event) {
			console.log("eventMenuAdd");
			$(".entry-add-modal").modal();
		});
		$("entry-add").on("click", function() {
			entryListView.addEntry();
		});
	}
});


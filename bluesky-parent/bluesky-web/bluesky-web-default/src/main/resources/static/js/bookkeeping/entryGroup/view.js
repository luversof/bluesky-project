$(document).ready(function() {

	$.EntryGroupView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		template : $("#template-entryGroup-view").html(),
		events : {
			"click [data-menu=updateEntryGroup]" : "updateEntryGroup",
			"click [data-menu=deleteEntryGroup]" : "deleteEntryGroup",
			"keyup [data-key-name=name]" : "nameKeyUp",
			"keypress [data-key-name=name]" : "nameKeyPress",
			"change select[name=entryType]" : "isChange"
		},
		initialize : function() {
			console.log("This view has been initialized.");
			this.listenTo(this.model, 'change', this.render);
			this.listenTo(this.model, 'destroy', this.remove);
		},
		render : function() {
			//console.log("EntryGroupView render", this.model, this.$el);
			var data = this.model.toJSON();
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			this.$el.find("select[name=entryType] > option[value=" + this.model.get("entryType") + "]").attr("selected", "selected");
			this.$el.find("[data-menu=updateEntryGroup]").hide();
			return this;
		},
		updateEntryGroup : function() {
			this.model.save({name : this.$el.find("[data-key-name=name]").text(), entryType : this.$el.find("select[name=entryType] option:selected").val()});
			this.$el.find("[data-menu=updateEntryGroup]").hide(100);
		},
		deleteEntryGroup : function() {
			this.model.destroy();
		},
		isChange : function() {
			if (this.$el.find("[data-key-name=name]").text() == this.model.get("name")
					&& this.$el.find("select[name=entryType] option:selected").val() == this.model.get("entryType")) {
				this.$el.find("[data-menu=updateEntryGroup]").hide(100);
			} else {
				this.$el.find("[data-menu=updateEntryGroup]").show(100);
			}
		},
		nameKeyUp : function(event) {
			this.isChange();
			if (event.keyCode == 13) {
				this.updateEntryGroup();
			}
		},
		// enter 입력 처리 방지
		nameKeyPress : function(event) {
			return event.keyCode != 13;
		}
	});


	$.EntryGroupCollectionView = Backbone.View.extend({
		el : "#entryGroupArea",
		template : $("#template-entryGroup-collection-view").html(),
		events : {
			"click [data-menu=createEntryGroup]" : "createEntryGroup",
			"keyup [data-key-name=createEntryGroupName]" : "createEntryGroupNameKeyUp",
			"keypress [data-key-name=createEntryGroupName]" : "createEntryGroupNameKeyPress"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.EntryGroupCollection();
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.render);
			
			this.collection.fetch({reset : true});
		},
		render : function() {
			this.$el.html(Mustache.render(this.template));
			this.collection.each(function(entryGroup) {
				var entryGroupView = new $.EntryGroupView({ model : entryGroup });
				this.$el.find("table tbody").append(entryGroupView.render().el);
			}, this);
			this.$el.find("option[value=TRANSFER]").hide();
		},
//		renderEntryGroup : function(entryGroup) {
//			var entryGroupView = new $.EntryGroupView({model : entryGroup});
//			this.$el.find("table tbody").append(entryGroupView.render().el);
//		},
		createEntryGroup : function(event) {
			event.preventDefault();
			var entryGroup = new $.EntryGroup({
				name : this.$el.find("[data-key-name=createEntryGroupName]").text(),
				entryType : this.$el.find("select[name=createEntryType] option:selected").val()
			});
			if (!entryGroup.isValid()) {
				return;
			}
			this.collection.create(entryGroup);
			$(event.currentTarget).closest("tr")
				.find("[contenteditable=true]").text("").end()
				.find("select option:eq(0)").attr("selected", "selected");
		},
		createEntryGroupNameKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createEntryGroup(event);
			}
		},
		createEntryGroupNameKeyPress : function(event) {
			return event.keyCode !== 13;
		}
	});

	$.entryGroupCollectionView = new $.EntryGroupCollectionView();
});
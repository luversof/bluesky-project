$(document).ready(function() {

	$.EntryGroupView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		//className : "testt",
		template : $("#template-entryGroup-view").html(),
		events : {
			"click [data-menu=updateEntryGroup]" : "updateEntryGroup",
			"click [data-menu=deleteEntryGroup]" : "deleteEntryGroup",
			"keyup [data-key=name]" : "changeNameKeyUP",
			"keypress [data-key=name]" : "changeNameKeyPress",
			"change select[name=entryType]" : "changeEntryType"
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
			this.model.save({name : this.$el.find("[data-key=name]").text(), entryType : this.$el.find("select[name=entryType] option:selected").val()});
			this.$el.find("[data-menu=updateEntryGroup]").hide(100);
		},
		deleteEntryGroup : function() {
			this.model.destroy();
		},
		changeNameKeyUP : function(event) {
			//console.log("data : ", this.$el.find("[data-key=name]").text());
			if (this.$el.find("[data-key=name]").text() == this.model.get("name")) {
				this.$el.find("[data-menu=updateEntryGroup]").hide(100);
			} else {
				this.$el.find("[data-menu=updateEntryGroup]").show(100);
			}
			if (event.keyCode == 13) {
				this.updateEntryGroup();
			}
		},
		// enter 입력 처리 방지
		changeNameKeyPress : function(event) {
			return event.keyCode != 13;
		},
		changeEntryType : function() {
			if (this.$el.find("select[name=entryType] option:selected").val() == this.model.get("entryType")) {
				this.$el.find("[data-menu=updateEntryGroup]").hide(100);
			} else {
				this.$el.find("[data-menu=updateEntryGroup]").show(100);
			}
		}
	});


	$.EntryGroupCollectionView = Backbone.View.extend({
		el : "#entryGroupArea",
		template : $("#template-entryGroup-list").html(),
		events : {
			"click [data-menu=createEntryGroup]" : "createEntryGroup",
			"keyup [data-key-name=createEntryGroupName]" : "createNameKeyUp",
			"keypress [data-key-name=createEntryGroupName]" : "createNameKeyPress"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.$el.html(Mustache.render(this.template));
			
			this.collection = new $.EntryGroupCollection();
			this.collection.fetch({reset : true});
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderEntryGroup);
		},
		render : function() {
			this.collection.each(function(entryGroup) {
				var entryGroupView = new $.EntryGroupView({model : entryGroup});
				this.$el.find("table tbody").append(entryGroupView.render().el);
			}, this);
		},
		renderEntryGroup : function(entryGroup) {
			var entryGroupView = new $.EntryGroupView({model : entryGroup});
			this.$el.find("table tbody").append(entryGroupView.render().el);
		},
		createEntryGroup : function(event) {
			event.preventDefault();
			this.collection.create({ name : $("[data-key-name=createEntryGroupName]").text(), entryType : $("select[name=createEntryType] option:selected").val() });
			$(event.target).closest("tr")
				.find("[contenteditable=true]").text("").end()
				.find("select option:eq(0)").attr("selected", "selected");
			//this.collection.create({ name : $("[data-key-name=createEntryGroupName]").text(), entryType : $("select[name=createEntryType] option:selected").val() });
		},
		createNameKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createEntryGroup(event);
			}
		},
		createNameKeyPress : function(event) {
			return event.keyCode !== 13;
		}
	});

	$.entryGroupCollectionView = new $.EntryGroupCollectionView();
});
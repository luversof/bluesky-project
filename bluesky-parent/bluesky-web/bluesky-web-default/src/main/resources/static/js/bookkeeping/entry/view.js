$(document).ready(function() {

	$.EntryView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		//className : "testt",
		template : $("#template-entry-view").html(),
		events : {
			"click [data-menu=updateEntry]" : "updateEntry",
			"click [data-menu=deleteEntry]" : "deleteEntry",
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
			//console.log("EntryView render", this.model, this.$el);
			var data = this.model.toJSON();
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			this.$el.find("select[name=entryType] > option[value=" + this.model.get("entryType") + "]").attr("selected", "selected");
			this.$el.find("[data-menu=updateEntry]").hide();
			return this;
		},
		updateEntry : function() {
			this.model.save({name : this.$el.find("[data-key=name]").text(), entryType : this.$el.find("select[name=entryType] option:selected").val()});
			this.$el.find("[data-menu=updateEntry]").hide(100);
		},
		deleteEntry : function() {
			this.model.destroy();
		},
		changeNameKeyUP : function(event) {
			//console.log("data : ", this.$el.find("[data-key=name]").text());
			if (this.$el.find("[data-key=name]").text() == this.model.get("name")) {
				this.$el.find("[data-menu=updateEntry]").hide(100);
			} else {
				this.$el.find("[data-menu=updateEntry]").show(100);
			}
			if (event.keyCode == 13) {
				this.updateEntry();
			}
		},
		// enter 입력 처리 방지
		changeNameKeyPress : function(event) {
			return event.keyCode != 13;
		},
		changeEntryType : function() {
			if (this.$el.find("select[name=entryType] option:selected").val() == this.model.get("entryType")) {
				this.$el.find("[data-menu=updateEntry]").hide(100);
			} else {
				this.$el.find("[data-menu=updateEntry]").show(100);
			}
		}
	});


	$.EntryCollectionView = Backbone.View.extend({
		el : "#entryArea",
		template : $("#template-entry-list").html(),
		events : {
			"click [data-menu=createEntry]" : "createEntry",
			"keyup [data-key-name=createEntryName]" : "createNameKeyUp",
			"keypress [data-key-name=createEntryName]" : "createNameKeyPress"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.EntryCollection();
			this.entryGroupCollection = new $.EntryGroupCollection();
			this.assetCollection = new $.AssetCollection();
			
			this.listenTo(this.entryGroupCollection, "reset", this.render);
			this.listenTo(this.assetCollection, "reset", this.render);
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderEntry);
			
			this.entryGroupCollection.fetch({reset : true});
			this.assetCollection.fetch({reset : true});
			this.collection.fetch({reset : true});
		},
		render : function() {
			console.log("Test : ", this.assetCollection.toJSON());
			var data = {
				assetList : this.assetCollection.toJSON(),
				entryGroupList : this.entryGroupCollection.toJSON()
			};
			this.$el.html(Mustache.render(this.template, data));
			this.collection.each(function(entry) {
				var entryView = new $.EntryView({model : entry});
				this.$el.find("table tbody").append(entryView.render().el);
			}, this);
		},
		renderEntry : function(entry) {
			var entryView = new $.EntryView({model : entry});
			this.$el.find("table tbody").append(entryView.render().el);
		},
		createEntry : function(event) {
			event.preventDefault();
			this.collection.create({ name : $("[data-key-name=createEntryName]").text(), entryType : $("select[name=createEntryType] option:selected").val() });
			$(event.target).closest("tr")
				.find("[contenteditable=true]").text("").end()
				.find("select option:eq(0)").attr("selected", "selected");
			//this.collection.create({ name : $("[data-key-name=createEntryName]").text(), entryType : $("select[name=createEntryType] option:selected").val() });
		},
		createNameKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createEntry(event);
			}
		},
		createNameKeyPress : function(event) {
			return event.keyCode !== 13;
		}
	});

	$.entryCollectionView = new $.EntryCollectionView();
});
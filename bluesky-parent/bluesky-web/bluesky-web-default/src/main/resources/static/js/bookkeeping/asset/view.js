$(document).ready(function() {

	$.AssetView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		template : $("#template-asset-view").html(),
		events : {
			"click [data-menu=updateAsset]" : "updateAsset",
			"click [data-menu=deleteAsset]" : "deleteAsset",
			"keyup [data-key=name]" : "changeNameKeyUp",
			"keypress [data-key=name]" : "changeNameKeyPress",
			"change select[name=assetType]" : "isChange"
		},
		initialize : function() {
			console.log("This view has been initialized.");
			this.listenTo(this.model, 'change', this.render);
			this.listenTo(this.model, 'destroy', this.remove);
		},
		render : function() {
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			this.$el.find("select[name=assetType] > option[value=" + this.model.get("assetType") + "]").attr("selected", "selected");
			this.$el.find("[data-menu=updateAsset]").hide();
			return this;
		},
		updateAsset : function() {
			this.model.save({name : this.$el.find("[data-key=name]").text(), assetType : this.$el.find("select[name=assetType] option:selected").val()});
			this.$el.find("[data-menu=updateAsset]").hide(100);
		},
		deleteAsset : function() {
			this.model.destroy();
		},
		isChange : function() {
			if (this.$el.find("[data-key=name]").text() == this.model.get("name")
					&& this.$el.find("select[name=assetType] option:selected").val() == this.model.get("assetType")) {
				this.$el.find("[data-menu=updateAsset]").hide(100);
			} else {
				this.$el.find("[data-menu=updateAsset]").show(100);
			}
		},
		changeNameKeyUp : function(event) {
			this.isChange();
			if (event.keyCode == 13) {
				this.updateAsset();
			}
		},
		// enter 입력 처리 방지
		changeNameKeyPress : function(event) {
			return event.keyCode != 13;
		}
	});


	$.AssetCollectionView = Backbone.View.extend({
		el : "#assetArea",
		template : $("#template-asset-list").html(),
		events : {
			"click [data-menu=createAsset]" : "createAsset",
			"keyup [data-key-name=createAssetName]" : "createNameKeyUp",
			"keypress [data-key-name=createAssetName]" : "createNameKeyPress"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.AssetCollection();
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderAsset);
			
			this.collection.fetch({reset : true});
		},
		render : function() {
			this.$el.html(Mustache.render(this.template));
			this.collection.each(function(asset) {
				var assetView = new $.AssetView({model : asset});
				this.$el.find("table tbody").append(assetView.render().el);
			}, this);
		},
		renderAsset : function(asset) {
			var assetView = new $.AssetView({model : asset});
			this.$el.find("table tbody").append(assetView.render().el);
		},
		createAsset : function(event) {
			event.preventDefault();
			var asset = new $.Asset({
				name : this.$el.find("[data-key-name=createAssetName]").text(),
				assetType : this.$el.find("select[name=createAssetType] option:selected").val()
			});
			if (!asset.isValid()) {
				return;
			}
			this.collection.create(asset);
			$(event.target).closest("tr")
				.find("[contenteditable=true]").text("").end()
				.find("select option:eq(0)").attr("selected", "selected");
		},
		createNameKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createAsset(event);
			}
		},
		createNameKeyPress : function(event) {
			return event.keyCode !== 13;
		}
	});

	$.assetCollectionView = new $.AssetCollectionView();
});
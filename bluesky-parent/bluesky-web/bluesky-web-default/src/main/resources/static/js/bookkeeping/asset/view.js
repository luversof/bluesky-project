$(document).ready(function() {

	$.AssetView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		template : $("#template-asset-view").html(),
		events : {
			"click [data-menu=updateAsset]" : "updateAsset",
			"click [data-menu=deleteAsset]" : "deleteAsset",
			"keyup [data-key-name=name]" : "nameKeyUp",
			"keypress [data-key-name=name]" : "nameKeyPress",
			"change select[name=assetType]" : "isChange"
		},
		initialize : function() {
			// console.log("This view has been initialized.");
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
			this.model.save({name : this.$el.find("[data-key-name=name]").text(), assetType : this.$el.find("select[name=assetType] option:selected").val()});
			this.$el.find("[data-menu=updateAsset]").hide(100);
		},
		deleteAsset : function() {
			this.model.destroy();
		},
		isChange : function() {
			if (this.$el.find("[data-key-name=name]").text() == this.model.get("name")
					&& this.$el.find("select[name=assetType] option:selected").val() == this.model.get("assetType")) {
				this.$el.find("[data-menu=updateAsset]").hide(100);
			} else {
				this.$el.find("[data-menu=updateAsset]").show(100);
			}
		},
		nameKeyUp : function(event) {
			this.isChange();
			if (event.keyCode == 13) {
				this.updateAsset();
			}
		},
		// enter 입력 처리 방지
		nameKeyPress : function(event) {
			return event.keyCode != 13;
		}
	});


	$.AssetCollectionView = Backbone.View.extend({
		el : "#assetArea",
		template : $("#template-asset-collection-view").html(),
		events : {
			"click [data-menu=createAsset]" : "createAsset",
			"keyup [data-key-name=createAssetName]" : "createAssetNameKeyUp",
			"keypress [data-key-name=createAssetName]" : "createAssetNameKeyPress",
			"click [data-menu-sortColumn][data-menu-sortDirection]" : "renderBySortColumn"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.AssetCollection();
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.render);
			this.listenTo(this.collection, "sort", this.render);
			
			this.collection.fetch({reset : true});
		},
		render : function() {
			var data = {
				sortColumn : this.collection.sortColumn,
				sortDirection : this.collection.sortDirection
			};
			data.isSortColumnAssetType = function() {
				return this.sortColumn == "assetType";
			}
			data.isSortColumnName = function() {
				return this.sortColumn == "name";
			}
			data.isSortColumnAmount = function() {
				return this.sortColumn == "amount";
			}
			this.$el.html(Mustache.render(this.template, data));
			this.collection.each(function(asset) {
				var assetView = new $.AssetView({ model : asset });
				this.$el.find("table tbody").append(assetView.render().el);
			}, this);
		},
//		renderAsset : function(asset) {
//			var assetView = new $.AssetView({model : asset});
//			this.$el.find("table tbody").append(assetView.render().el);
//		},
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
			$(event.currentTarget).closest("tr")
				.find("[contenteditable=true]").text("").end()
				.find("select option:eq(0)").attr("selected", "selected");
		},
		createAssetNameKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createAsset(event);
			}
		},
		createAssetNameKeyPress : function(event) {
			return event.keyCode !== 13;
		}
	});

	$.assetCollectionView = new $.AssetCollectionView();
});
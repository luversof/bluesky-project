$(document).ready(function() {
	
	$.AssetView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		//className : "testt",
		template : $("#template-asset-view").html(),
		events : {
			"click .glyphicon-edit" : "updateAsset",
			"click .glyphicon-remove" : "removeAsset",
			"keyup [data-key=name]" : "changeNameKeyUP",
			"keypress [data-key=name]" : "changeNameKeyPress"
		},
		initialize : function() {
			console.log("This view has been initialized.");
			this.listenTo(this.model, 'change', this.render);
			this.listenTo(this.model, 'destroy', this.remove);
		},
		render : function() {
			//console.log("AssetView render", this.model, this.$el);
			var data = this.model.toJSON();
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			this.$el.find("select[name=assetType] > option[value=" + this.model.assetType + "]").attr("selected", "selected");
			this.$el.find(".glyphicon-edit").hide();
			return this;
		},
		edit : function() {
			console.log("edit");
		},
		updateAsset : function() {
			this.model.save({name : this.$el.find("[data-key=name]").text()});
			this.$el.find(".glyphicon-edit").hide(100);
		},
		removeAsset : function() {
			this.model.destroy();
		},
		changeNameKeyUP : function(e) {
			//console.log("data : ", this.$el.find("[data-key=name]").text());
			if (this.$el.find("[data-key=name]").text() != this.model.get("name")) {
				this.$el.find(".glyphicon-edit").show(100);
			} else {
				this.$el.find(".glyphicon-edit").hide(100);
			}
			if (e.keyCode == 13) {
				this.updateAsset();
			}
		},
		// enter 입력 처리 방지
		changeNameKeyPress : function(e) {
			return e.keyCode != 13;
		}
	});
	
	
	$.AssetCollectionView = Backbone.View.extend({
		el : "#assetArea table tbody",
		template : $("#template-asset-list").html(),
		events : {
			//"click .addAsset" : "addAsset"
		},
		initialize : function() {
			console.log("This collection view has been initialized.");
			
			this.collection = new $.AssetCollection();
			this.collection.fetch({reset : true});
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderAsset);
		},
		render : function() {
			console.log("AssetCollectionView render, model : ", this.collection.toJSON());
			
			this.collection.each(function(asset) {
				var assetView = new $.AssetView({model : asset});
				this.$el.append(assetView.render().el);
			}, this);
			
			//Mustache의 하위 참조 방식을 사용하지 않음.
			//this.$el.html(Mustache.render(this.assetCollectionTemplate, this.collection.toJSON(), {"asset-view" : $.AssetView.prototype.template}))
			//return this;
		},
		renderAsset : function(asset) {
			var assetView = new $.AssetView({model : asset});
			this.$el.append(assetView.render().el);
		},
		addAsset : function() {
			
			console.log("addAsset", this.collection);
			this.collection.create({ name : $("input[name=addAssetName]").val(), assetType : $("select[name=addAssetType] option:selected").val() });
		}
	});

	$("#assetArea").html(Mustache.render($("#template-asset-list").html()));
	
	$.assetCollectionView = new $.AssetCollectionView();
	
	
	$(document).on("click", ".addAsset", function(event) {
		event.preventDefault();
		$.assetCollectionView.addAsset();
	});
});
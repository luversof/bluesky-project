$(document).ready(function() {
	// entry 호출 전 필요한 데이터 호출
	var entryGroupCollection = new $.EntryGroupCollection();
	//var assetCollection = new $.AssetCollection();
	
	entryGroupCollection.fetch({ async : false });
	//assetCollection.fetch({ async : false });
	
	$.StatisticsView = Backbone.View.extend({
		el : "<tr>",	//기본은 div,
		template : $("#template-statistics-view").html(),
		events : {
			
		},
		initialize : function() {
//			console.log("This view has been initialized.");
			this.listenTo(this.model, "change", this.render);
			this.listenTo(this.model, "destroy", this.remove);
		},
		render : function() {
//			console.log("EntryView render", this.model, this.model.collection);
			var data = {
				entry :	this.model.toJSON(),
				//assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON()
			}
			
			this.$el.html(Mustache.render(this.template, data));
			return this;
		}
	});
	
	$.StatisticsCollectionView = Backbone.View.extend({
		el : "#statisticsArea",
		template : $("#template-statistics-collection-view").html(),
		events : {
			
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.StatisticsCollection();

			this.statisticsSearchInfo = new $.StatisticsSearchInfo();
			var statisticsSearchInfoView = new $.StatisticsSearchInfoView({ model : this.statisticsSearchInfo });
			this.statisticsSearchInfo.fetch({ data : { chronoUnit : "YEARS" }});
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.statisticsSearchInfo, "change", this.changeStatisticsSearchInfo);
		},
		render : function() {
			var data = {
				//assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON(),
				statisticsSearchInfo : this.statisticsSearchInfo.toJSON()
			};
			var statisticsList = this.collection.toJSON();
			this.$el.html(Mustache.render(this.template, data));
			this.collection.each(function(entry) {
				var statisticsView = new $.StatisticsView({ model : entry });
//				entryView.assetList = data.assetList;
//				entryView.entryGroupList = data.entryGroupList;
				this.$el.find("table tbody").append(statisticsView.render().el);
			}, this);
		},
		changeStatisticsSearchInfo : function() {
//			console.log("changeEntrySearchInfo ");
			this.collection.sortColumn = "entryDate";
			this.collection.sortDirection = "asc";
			this.collection.fetch({
				reset : true,
				data : $.param({
					chronoUnit : this.statisticsSearchInfo.get("chronoUnit"),
					targetLocalDate : this.statisticsSearchInfo.get("targetLocalDate")
				})
			});
		}
	});
	
	$.statisticsCollectionView = new $.StatisticsCollectionView();
});
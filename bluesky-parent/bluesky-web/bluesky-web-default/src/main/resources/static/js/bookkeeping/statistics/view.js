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
//			console.log("StatisticsView render", this.model, this.model.collection);
			var data = {
				statistics : this.model.toJSON(),
				//assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON()
			};
			
			//entryType을 먼저 확인
			var entryType = this.model.get("entryGroup").entryType;
		
			data.isTargetEntryGroup = function() {
				return this.entryType === entryType;
			};
			data.isCredit = function() {
				return entryType === "CREDIT";
			};
			data.isDebit = function() {
				return entryType === "DEBIT";
			};
			data.statisticsAmountFormat = function() {
				return numeral(this.statistics.amount).format();
			};
			
			data.statisticsAmountPercent = function() {
				return "??%";
			};
			
			this.$el.html(Mustache.render(this.template, data));
			return this;
		}
	});
	
	$.StatisticsCollectionView = Backbone.View.extend({
		el : "#statisticsArea",
		template : $("#template-statistics-collection-view").html(),
		events : {
			"click [data-menu-sortColumn][data-menu-sortDirection]" : "renderBySortColumn",
			"click [data-menu-selectStatisticsDisplay]" : "selectStatisticsDisplay"
		},
		displayEntryType : "TOTAL", 
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.StatisticsCollection();

			this.statisticsSearchInfo = new $.StatisticsSearchInfo();
			//var statisticsSearchInfoView = new $.StatisticsSearchInfoView({ model : this.statisticsSearchInfo });
			new $.StatisticsSearchInfoView({ model : this.statisticsSearchInfo });
			this.statisticsSearchInfo.fetch({ data : { chronoUnit : "YEARS" }});
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "sort", this.render);
			this.listenTo(this.statisticsSearchInfo, "change", this.changeStatisticsSearchInfo);
		},
		render : function() {
			var data = {
				//assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON(),
				statisticsSearchInfo : this.statisticsSearchInfo.toJSON(),
				sortColumn : this.collection.sortColumn,
				sortDirection : this.collection.sortDirection
			};
			var statisticsList = this.collection.toJSON();
			var format = "0,0[.]00$";
			var displayEntryType = this.displayEntryType;
			
			data.getTotalCreditAmount = function() {
				return _.reduce(statisticsList, function(amount, statistics) {
					return numeral(numeral().unformat(amount) + (statistics.entryGroup.entryType === "CREDIT" ? statistics.amount : 0)).format(format);
				}, 0);
			};
			data.getTotalDebitAmount = function() {
				return _.reduce(statisticsList, function(amount, statistics) {
					return numeral(numeral().unformat(amount) + (statistics.entryGroup.entryType === "DEBIT" ? statistics.amount : 0)).format(format);
				}, 0);
			};
			data.getTotalAmount = function() {
				return numeral(numeral().unformat(data.getTotalCreditAmount()) - numeral().unformat(data.getTotalDebitAmount())).format(format);
			};
			data.isSortEntryType = function() {
				return this.sortColumn === "entryType";
			};
			data.isSortColumnAmount = function() {
				return this.sortColumn === "amount";
			};
			data.isDisplayEntryTypeTotal = function() {
				return displayEntryType === "TOTAL";
			};
			data.isDisplayEntryTypeCredit = function() {
				return displayEntryType === "CREDIT";
			};
			data.isDisplayEntryTypeDebit = function() {
				return displayEntryType === "DEBIT";
			};
			
			this.$el.html(Mustache.render(this.template, data));
			this.collection.each(function(statistics) {
				if (this.displayEntryType !== "TOTAL" && this.displayEntryType !== statistics.get("entryType")) {
					return;
				}
				var statisticsView = new $.StatisticsView({ model : statistics });
//				entryView.assetList = data.assetList;
//				entryView.entryGroupList = data.entryGroupList;
				this.$el.find("table tbody").append(statisticsView.render().el);
			}, this);
			
			// 차트 표시
			$.displayChart(this.collection.toJSON(), this.displayEntryType);
		},
		changeStatisticsSearchInfo : function() {
//			console.log("changeEntrySearchInfo ");
			this.collection.fetch({
				reset : true,
				data : $.param({
					chronoUnit : this.statisticsSearchInfo.get("chronoUnit"),
					targetLocalDate : this.statisticsSearchInfo.get("targetLocalDate")
				})
			});
		},
		selectStatisticsDisplay : function(event) {
			this.displayEntryType = $(event.currentTarget).attr("data-menu-selectStatisticsDisplay");
			this.render();
		}
	});
	
	$.statisticsCollectionView = new $.StatisticsCollectionView();
});
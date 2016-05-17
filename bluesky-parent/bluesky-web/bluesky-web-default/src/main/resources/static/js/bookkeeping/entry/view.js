$(document).ready(function() {
	// 추가로 필요한 데이터 호출
	var entryGroupCollection = new $.EntryGroupCollection();
	var assetCollection = new $.AssetCollection();
	
	entryGroupCollection.fetch({ async : false });
	assetCollection.fetch({ async : false });
	
	$.EntryView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		className : "warning",
		template : $("#template-entry-view").html(),
		events : {
			"click [data-menu=updateEntry]" : "updateEntry",
			"click [data-menu=deleteEntry]" : "deleteEntry",
			"keyup [data-key-name=amount]" : "changeAmountKeyUp",
			"keypress [data-key-name=amount]" : "changeAmountKeyPress",
			"keyup [data-key-name=memo]" : "changeMemoKeyUp",
			"keypress [data-key-name=memo]" : "changeMemoKeyPress",
			"change select[name=entryGroup]" : "isChange",
			"change select[name=debitAsset]" : "isChange",
			"change select[name=creditAsset]" : "isChange",
			"change input[name=entryDate]" : "isChange"
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
				assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON()
			}
			data.getEntryDate = function() {
				return moment(new Date(data.entry.entryDate)).format("YYYY-MM-DD")
			}
			
			//entryType을 먼저 확인
			var entryType = this.model.get("entryType");
			
			data.isTargetEntryGroup = function() {
				return this.entryType == entryType;
			}
			data.isCredit = function() {
				return entryType == "CREDIT";
			}
			data.isDebit = function() {
				return entryType == "DEBIT";
			}
			data.isTransfer = function() {
				return entryType == "TRANSFER";
			}
			
			this.$el.html(Mustache.render(this.template, data));
			if (this.model.get("entryGroup") != null) { this.$el.find("select[name=entryGroup] > option[value=" + this.model.get("entryGroup").id + "]").attr("selected", "selected") };
			if (this.model.get("debitAsset") != null) { this.$el.find("select[name=debitAsset] > option[value=" + this.model.get("debitAsset").id + "]").attr("selected", "selected") };
			if (this.model.get("creditAsset") != null) { this.$el.find("select[name=creditAsset] > option[value=" + this.model.get("creditAsset").id + "]").attr("selected", "selected") };
			this.$el.find("[data-menu=updateEntry]").hide();
			
			//외부 모듈 이벤트 핸들링 추가
			this.$el.find("input[name=entryDate]").datepicker({ language: "ko", autoclose : true });
			return this;
		},
		updateEntry : function() {
			this.model.save({
				entryGroup : this.model.get("entryType") == "TRANSFER" ? null : { id : this.$el.find("select[name=entryGroup] option:selected").val() },
				debitAsset : this.model.get("entryType") == "CREDIT" ? null : { id : this.$el.find("select[name=debitAsset] option:selected").val() },
				creditAsset : this.model.get("entryType") == "DEBIT" ? null : { id : this.$el.find("select[name=creditAsset] option:selected").val() },
				amount : this.$el.find("[data-key-name=amount]").text(),
				memo : this.$el.find("[data-key-name=memo]").text(),
				entryDate : moment.utc(new Date(this.$el.find("input[name=entryDate]").val())).format("YYYY-MM-DDTHH:mm:ss")
			});
			this.$el.find("[data-menu=updateEntry]").hide(100);
		},
		deleteEntry : function() {
			this.model.destroy();
		},
		// 변경된 내용이 있는지 여부 확인
		isChange : function() {
			if (this.$el.find("[data-key-name=amount]").text() == this.model.get("amount") 
					&& this.$el.find("[data-key-name=memo]").text() == this.model.get("memo")
					&& (this.model.get("entryGroup") == null || this.$el.find("select[name=entryGroup] option:selected").val() == this.model.get("entryGroup").id)
					&& (this.model.get("debitAsset") == null || this.$el.find("select[name=debitAsset] option:selected").val() == this.model.get("debitAsset").id)
					&& (this.model.get("creditAsset") == null || this.$el.find("select[name=creditAsset] option:selected").val() == this.model.get("creditAsset").id)
					&& moment(this.$el.find("input[name=entryDate]").val()).format() == moment(this.model.get("entryDate")).format()) {
				this.$el.find("[data-menu=updateEntry]").hide(100);
			} else {
				this.$el.find("[data-menu=updateEntry]").show(100);
			}
		},
		changeAmountKeyUp : function(event) {
			this.isChange();
			if (event.keyCode == 13) {
				this.updateEntry();
			}
		},
		// enter 입력 처리 방지
		changeAmountKeyPress : function(event) {
			return event.keyCode != 13;
		},
		changeMemoKeyUp : function(event) {
			this.isChange();
			if (event.keyCode == 13) {
				this.updateEntry();
			}
		},
		// enter 입력 처리 방지
		changeAmountKeyPress : function(event) {
			return event.keyCode != 13;
		}
	});


	$.EntryCollectionView = Backbone.View.extend({
		el : "#entryArea",
		template : $("#template-entry-collection-view").html(),
		events : {
			"click [data-menu=createEntry]" : "createEntry",
			"keyup [data-key-name=createMemo]" : "createMemoKeyUp",
			"keypress [data-key-name=createMemo]" : "createMemoKeyPress",
			"keyup [data-key-name=createAmount]" : "createAmountKeyUp",
			"keypress [data-key-name=createAmount]" : "createAmountKeyPress",
			"click [data-menu=selectCreateEntryType]" : "selectCreateEntryType",
			"click [data-menu-sortColumn][data-menu-sortDirection]" : "renderBySortColumn"
			
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.EntryCollection();

			this.entrySearchInfo = new $.EntrySearchInfo();
			var entrySearchInfoView = new $.EntrySearchInfoView({ model : this.entrySearchInfo });
			this.entrySearchInfo.fetch();
			//this.entryGroupCollection = new $.EntryGroupCollection();
			//this.assetCollection = new $.AssetCollection();
			
			//this.listenTo(this.entryGroupCollection, "reset", this.render);
			//this.listenTo(this.assetCollection, "reset", this.render);
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.render);
			this.listenTo(this.collection, "sort", this.render);
			this.listenTo(this.entrySearchInfo, "change", this.changeEntrySearchInfo);
			
			//this.entryGroupCollection.fetch({reset : true});
			//this.assetCollection.fetch({reset : true});
			
			
		},
		render : function() {
			//console.log("EntryCollectionView render");
			var data = {
				assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON(),
				entrySearchInfo : this.entrySearchInfo.toJSON(),
				sortColumn : this.collection.sortColumn,
				sortDirection : this.collection.sortDirection
			};
			
			data.isSortEntryType = function() {
				return this.sortColumn == "entryType";
			}
			data.isSortColumnEntryDate = function() {
				return this.sortColumn == "entryDate";
			}
			data.isSortColumnAmount = function() {
				return this.sortColumn == "amount";
			}
			
			this.$el.html(Mustache.render(this.template, data));
			this.collection.each(function(entry) {
				var entryView = new $.EntryView({ model : entry });
//				entryView.assetList = data.assetList;
//				entryView.entryGroupList = data.entryGroupList;
				this.$el.find("table tbody").append(entryView.render().el);
			}, this);
			
			//외부 모듈 이벤트 핸들링 추가
			this.$el
				.find("input[name=createEntryDate]").datepicker({ language: "ko", autoclose : true }).end()
				.find("[data-menu=selectCreateEntryType]:eq(0)").trigger("click").end();
		},
//		renderEntry : function(entry) {
//			var entryView = new $.EntryView({ model : entry });
//			this.$el.find("table tbody").append(entryView.render().el);
//		},
		createEntry : function(event) {
			event.preventDefault();
			var entryType = this.$el.find("[data-menu=selectCreateEntryType].active").val();
			var entry = new $.Entry({
				entryGroup : entryType == "TRANSFER" ? null : { id : this.$el.find("select[name=createEntryGroup] option:selected").val() },
				debitAsset : entryType == "CREDIT" ? null : { id : this.$el.find("select[name=createDebitAsset] option:selected").val() },
				creditAsset : entryType == "DEBIT" ? null :  { id : this.$el.find("select[name=createCreditAsset] option:selected").val() },
				amount : this.$el.find("[data-key-name=createAmount]").text(),
				memo : this.$el.find("[data-key-name=createMemo]").text(),
				entryDate : moment.utc(new Date(this.$el.find("input[name=createEntryDate]").val())).format("YYYY-MM-DDTHH:mm:ss")
			});
			
			if (!entry.isValid()) {
				return;
			}
			this.collection.create(entry);
			$(event.currentTarget).closest("tr")
				.find("[contenteditable=true]").text("").end()
				.find("select option:eq(0)").attr("selected", "selected");
		},
		createAmountKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createEntry(event);
			}
		},
		createAmountKeyPress : function(event) {
			return event.keyCode !== 13;
		},
		createMemoKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createEntry(event);
			}
		},
		createMemoKeyPress : function(event) {
			return event.keyCode !== 13;
		},
		// 기입 유형 선택 관련
		selectCreateEntryType : function(event) {
			// 버튼 활성화 처리
			this.$el.find("[data-menu=selectCreateEntryType]").removeClass("active btn-info");
			$(event.currentTarget).addClass("active btn-info");
			
			var entryType = $(event.currentTarget).val();
			
			//선택한 entryType에 따라 입력 형태 변경 처리
			//입력 형태에 따른 entryGroup 처리 추가
			if (entryType == "CREDIT") {
				this.$el.find("[name=createEntryGroup]").closest("td").show();
				this.$el.find("[name=createDebitAsset]").attr("disabled", true).closest("td").hide();
				this.$el.find("[name=createCreditAsset]").removeAttr("disabled").closest("td").show();
			} else if (entryType == "DEBIT") {
				this.$el.find("[name=createEntryGroup]").closest("td").show();
				this.$el.find("[name=createDebitAsset]").removeAttr("disabled").closest("td").show();
				this.$el.find("[name=createCreditAsset]").attr("disabled", true).closest("td").hide();
			} else if (entryType == "TRANSFER") {
				this.$el.find("[name=createEntryGroup]").closest("td").hide();
				this.$el.find("[name=createDebitAsset]").removeAttr("disabled").closest("td").show();
				this.$el.find("[name=createCreditAsset]").removeAttr("disabled").closest("td").show();
			}
			
			if (entryType != "TRANSFER") {
				this.$el.find("[name=createEntryGroup]")
				.find("option").removeAttr("selected").each(function() {
					if (entryGroupCollection.get($(this).val()).get("entryType") == entryType){
						$(this).show().removeAttr("disabled");
					} else {
						$(this).hide().attr("disabled", true);
					}
				}).end()
				.find("option:not(:disabled):eq(0)").attr("selected", true);
			}
		},
		renderBySortColumn : function(event) {
			this.collection.sortColumn = $(event.currentTarget).attr("data-menu-sortColumn");
			this.collection.sortDirection = $(event.currentTarget).attr("data-menu-sortDirection") == "desc" ? "asc" : "desc";
			this.collection.sort();
		},
		changeEntrySearchInfo : function() {
//			console.log("changeEntrySearchInfo ");
			this.collection.sortColumn = "entryDate";
			this.collection.sortDirection = "asc";
			this.collection.fetch({
				reset : true,
				data : $.param({
					targetLocalDate : this.entrySearchInfo.get("targetLocalDate")
				})
			});
		}
	});

	$.entryCollectionView = new $.EntryCollectionView();
});
$(document).ready(function() {
	// 추가로 필요한 데이터 호출
	var entryGroupCollection = new $.EntryGroupCollection();
	var assetCollection = new $.AssetCollection();
	
	entryGroupCollection.fetch();
	assetCollection.fetch();

	$.EntryView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		template : $("#template-entry-view").html(),
		events : {
			"click [data-menu=updateEntry]" : "updateEntry",
			"click [data-menu=deleteEntry]" : "deleteEntry",
			"keyup [data-key=amount]" : "changeAmountKeyUp",
			"keypress [data-key=amount]" : "changeAmountKeyPress",
			"keyup [data-key=memo]" : "changeMemoKeyUp",
			"keypress [data-key=memo]" : "changeMemoKeyPress",
			"change select[name=entryGroup]" : "isChange",
			"change select[name=debitAsset]" : "isChange",
			"change select[name=creditAsset]" : "isChange"
		},
		initialize : function() {
//			console.log("This view has been initialized.");
			this.listenTo(this.model, 'change', this.render);
			this.listenTo(this.model, 'destroy', this.remove);
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
			var entryType = null;
			
			data.isTargetEntryGroup = function() {
				
				console.log("this : ", this);
				return true;
			}
			
			this.$el.html(Mustache.render(this.template, data));
			if (this.model.get("entryGroup") != null) { this.$el.find("select[name=entryGroup] > option[value=" + this.model.get("entryGroup").id + "]").attr("selected", "selected") };
			if (this.model.get("debitAsset") != null) { this.$el.find("select[name=debitAsset] > option[value=" + this.model.get("debitAsset").id + "]").attr("selected", "selected") };
			if (this.model.get("creditAsset") != null) { this.$el.find("select[name=creditAsset] > option[value=" + this.model.get("creditAsset").id + "]").attr("selected", "selected") };
			this.$el.find("[data-menu=updateEntry]").hide();
			
			//외부 모듈 이벤트 핸들링 추가
			this.$el.find("input[name=entryDate]").datepicker({ language: 'ko' });
			return this;
		},
		updateEntry : function() {
			this.model.save({
				entryGroup : { id : this.$el.find("select[name=entryGroup] option:selected").val() },
				debitAsset : { id : this.$el.find("select[name=debitAsset] option:selected").val() },
				creditAsset : { id : this.$el.find("select[name=creditAsset] option:selected").val() },
				amount : this.$el.find("[data-key=amount]").text(),
				memo : this.$el.find("[data-key=memo]").text(),
				entryDate : moment.utc(new Date(this.$el.find("input[name=entryDate]").val())).format("YYYY-MM-DDTHH:mm:ss")
			});
			this.$el.find("[data-menu=updateEntry]").hide(100);
		},
		deleteEntry : function() {
			this.model.destroy();
		},
		// 변경된 내용이 있는지 여부 확인
		isChange : function() {
			if (this.$el.find("[data-key=amount]").text() == this.model.get("amount") 
					&& this.$el.find("[data-key=memo]").text() == this.model.get("memo")
					&& this.$el.find("select[name=entryGroup] option:selected").val() == this.model.get("entryGroup").id
					&& this.$el.find("select[name=debitAsset] option:selected").val() == this.model.get("debitAsset").id
					&& this.$el.find("select[name=creditAsset] option:selected").val() == this.model.get("creditAsset").id) {
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
		template : $("#template-entry-list").html(),
		events : {
			"click [data-menu=createEntry]" : "createEntry",
			"keyup [data-key-name=createMemo]" : "createMemoKeyUp",
			"keypress [data-key-name=createMemo]" : "createMemoKeyPress",
			"keyup [data-key-name=createAmount]" : "createAmountKeyUp",
			"keypress [data-key-name=createAmount]" : "createAmountKeyPress",
			"click [data-menu=selectCreateEntryType]" : "selectCreateEntryType"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.EntryCollection();
			//this.entryGroupCollection = new $.EntryGroupCollection();
			//this.assetCollection = new $.AssetCollection();
			
			//this.listenTo(this.entryGroupCollection, "reset", this.render);
			//this.listenTo(this.assetCollection, "reset", this.render);
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderEntry);
			
			//this.entryGroupCollection.fetch({reset : true});
			//this.assetCollection.fetch({reset : true});
			this.collection.fetch({reset : true, data : $.param({page : 12})});
			
		},
		render : function() {
			var data = {
				assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON()
			};
			this.$el.html(Mustache.render(this.template, data));
			this.collection.each(function(entry) {
				var entryView = new $.EntryView({model : entry});
//				entryView.assetList = data.assetList;
//				entryView.entryGroupList = data.entryGroupList;
				this.$el.find("table tbody").append(entryView.render().el);
			}, this);
			
			//외부 모듈 이벤트 핸들링 추가
			this.$el.find("input[name=createEntryDate]").datepicker({ language: 'ko' });
		},
		renderEntry : function(entry) {
			var entryView = new $.EntryView({ model : entry });
			this.$el.find("table tbody").append(entryView.render().el);
		},
		createEntry : function(event) {
			event.preventDefault();
			var entry = new $.Entry({
				entryGroup : { id : $("select[name=createEntryGroup] option:selected").val() },
				debitAsset : { id : $("select[name=createDebitAsset] option:selected").val() },
				creditAsset : { id : $("select[name=createCreditAsset] option:selected").val() },
				amount : $("[data-key-name=createAmount]").text(),
				memo : $("[data-key-name=createMemo]").text(),
				entryDate : moment.utc(new Date($("input[name=createEntryDate").val())).format("YYYY-MM-DDTHH:mm:ss")
			});
			if (!entry.isValid()) {
				return;
			}
			this.collection.create(entry);
			$(event.target).closest("tr")
				.find("[contenteditable=true]").text("").end()
				.find("select option:eq(0)").attr("selected", "selected");
			//this.collection.create({ name : $("[data-key-name=createMemo]").text(), entryType : $("select[name=createEntryType] option:selected").val() });
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
			console.log("Test : ", event, event.target, $(event.target).val());
			// 버튼 활성화 처리
			$("[data-menu=selectCreateEntryType]").removeClass("active");
			$(event.target).addClass("active");
			
			var entryType = $(event.target).val();
			
			//선택한 entryType에 따라 입력 형태 변경 처리
			//입력 형태에 따른 entryGroup 처리 추가
			if (entryType == "credit") {
				$("[name=createDebitAsset]").attr("disabled", true).hide();
				$("[name=createCreditAsset]").removeAttr("disabled").show();
			} else if (entryType == "debit") {
				$("[name=createDebitAsset]").removeAttr("disabled").show();
				$("[name=createCreditAsset]").attr("disabled", true).hide();
			} else if (entryType == "transfer") {
				$("[name=createDebitAsset]").removeAttr("disabled").show();
				$("[name=createCreditAsset]").removeAttr("disabled").show();
			}
			
			
//			if (this.$el.find("select[name=entryType] option:selected").val() == this.model.get("entryType")) {
//				this.$el.find("[data-menu=updateEntry]").hide(100);
//			} else {
//				this.$el.find("[data-menu=updateEntry]").show(100);
//			}
		}
	});

	$.entryCollectionView = new $.EntryCollectionView();
});
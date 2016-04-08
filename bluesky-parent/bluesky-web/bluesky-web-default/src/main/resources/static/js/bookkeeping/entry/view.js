$(document).ready(function() {
	// 추가로 필요한 데이터 호출
	var entryGroupCollection = new $.EntryGroupCollection();
	var assetCollection = new $.AssetCollection();
	
	entryGroupCollection.fetch();
	assetCollection.fetch();

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
			console.log("EntryView render", this.model, this.model.collection);
			var data = {
				entry :	this.model.toJSON(),
				assetList : assetCollection.toJSON(),
				entryGroupList : entryGroupCollection.toJSON()
			}
			
			this.$el.html(Mustache.render(this.template, data));
			this.$el.find("select[name=entryGroup] > option[value=" + this.model.get("entryGroup").id + "]").attr("selected", "selected");
			this.$el.find("select[name=debitAsset] > option[value=" + this.model.get("debitAsset").id + "]").attr("selected", "selected");
			this.$el.find("select[name=creditAsset] > option[value=" + this.model.get("creditAsset").id + "]").attr("selected", "selected");
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
			"keyup [data-key-name=createMemo]" : "createMemoKeyUp",
			"keypress [data-key-name=createMemo]" : "createMemoKeyPress",
			"keyup [data-key-name=createAmount]" : "createAmountKeyUp",
			"keypress [data-key-name=createAmount]" : "createAmountKeyPress"
		},
		initialize : function() {
			this.test = "테스트";
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
			this.collection.fetch({reset : true});
			
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
		}
	});

	$.entryCollectionView = new $.EntryCollectionView();
});
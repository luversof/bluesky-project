$(document).ready(function() {
	
	$.EntrySearchInfoView = Backbone.View.extend({
		el : "[data-menu-area=entrysearchInfo]",
		template : $("#template-entrySearchInfo-view").html(),
		events : {
			"change input[name=entrySearchInfoTargetMonth]" : "isChange",
			"click [data-menu-entrySearchInfo]" : "selectMenu"
		},
		initialize : function() {
//			console.log("This view has been initialized.");
			this.listenTo(this.model, "change", this.render);
			this.listenTo(this.model, "reset", this.render);
		},
		render : function() {
//			console.log("EntrySearchInfoView render")
			var data = this.model.toJSON();
			data.getTargetMonth = function() {
				return moment(data.startDateTime).format("YYYY-MM");
			}
			this.$el
				.html(Mustache.render(this.template, data))
				.find("input[name=entrySearchInfoTargetMonth]").datepicker({ format : "yyyy-mm", language: "ko", minViewMode : 1, autoclose : true })
			return this;
		},
		selectEntrySearchInfo : function(targetLocalDate) {
			this.model.fetch({
				data : $.param({
					targetLocalDate : targetLocalDate
				})
			});
		},
		isChange : function() {
//			console.log("EntrySearchInfoView isChange");
			var targetMonth = this.$el.find("input[name=entrySearchInfoTargetMonth]").val();
			if (targetMonth != moment(this.model.get("startDateTime")).format("YYYY-MM")) {
				// 변경처리
				var targetLocalDate = moment(targetMonth).add(this.model.get("baseDate") - 1, "days").format("YYYY-MM-DD");
				this.selectEntrySearchInfo(targetLocalDate);
			}
		},
		selectMenu : function(event) {
			event.preventDefault();
			var menu = $(event.currentTarget).attr("data-menu-entrySearchInfo");
			var targetMonth = this.$el.find("input[name=entrySearchInfoTargetMonth]").val();
			var targetLocalDateMoment = moment(targetMonth).add(this.model.get("baseDate") - 1, "days");
			if (menu == "prevMonth") {
				targetLocalDateMoment.subtract(1, "months");
			} else if (menu == "nextMonth") {
				targetLocalDateMoment.add(1, "months");
			}
			this.selectEntrySearchInfo(targetLocalDateMoment.format("YYYY-MM-DD"));
		}
	});
});
$(document).ready(function() {
	
	$.EntrySearchInfoView = Backbone.View.extend({
		el : "[data-menu-area=entrysearchInfo]",
		template : $("#template-entrySearchInfo-view").html(),
		events : {
			"change input[name=entrySearchInfoTargetDate]" : "isChange",
			"click [data-menu-entrySearchInfo]" : "selectMenu"
		},
		initialize : function() {
			//console.log("This view has been initialized.");
			this.listenTo(this.model, "change", this.render);
			this.listenTo(this.model, "reset", this.render);
		},
		render : function() {
			//console.log("EntrySearchInfoView render")
			var data = this.model.toJSON();
			// entrySearchInfo는 월단위로 표시를 고정함
			data.getTargetDate = function() {
				return moment(data.startLocalDateTime).format(data.getMomentDateFormat());
			}
			this.$el
				.html(Mustache.render(this.template, data))
				.find("input[name=entrySearchInfoTargetDate]").datepicker({ format : data.getDatepickerDateFormat(), language: "ko", minViewMode : 1, autoclose : true })
			return this;
		},
		selectEntrySearchInfo : function(targetLocalDate) {
			this.model.fetch({
				reset : true,
				data : $.param({
					targetLocalDate : targetLocalDate
				})
			});
		},
		isChange : function() {
			//console.log("EntrySearchInfoView isChange");
			var targetDate = this.$el.find("input[name=entrySearchInfoTargetDate]").val();
			var data = this.model.toJSON();
			if (targetDate != moment(this.model.get("startLocalDateTime")).format(data.getMomentDateFormat())) {
				// 변경처리
				var targetLocalDate = moment(targetDate).add(this.model.get("baseDate") - 1, "days").format("YYYY-MM-DD");
				this.selectEntrySearchInfo(targetLocalDate);
			}
		},
		selectMenu : function(event) {
			event.preventDefault();
			var menu = $(event.currentTarget).attr("data-menu-entrySearchInfo");
			var targetDate = this.$el.find("input[name=entrySearchInfoTargetDate]").val();
			var data = this.model.toJSON();
			var targetLocalDateMoment = moment(targetDate).add(this.model.get("baseDate") - 1, "days");
			if (menu == "prevDate") {
				targetLocalDateMoment.subtract(1, data.getMomentManipulateKey());
			} else if (menu == "nextDate") {
				targetLocalDateMoment.add(1, data.getMomentManipulateKey());
			}
			this.selectEntrySearchInfo(targetLocalDateMoment.format("YYYY-MM-DD"));
		}
	});
});
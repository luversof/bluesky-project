$(document).ready(function() {
	
	$.StatisticsSearchInfoView = Backbone.View.extend({
		el : "[data-menu-area=statisticsSearchInfo]",
		template : $("#template-statisticsSearchInfo-view").html(),
		events : {
			"change input[name=statisticsSearchInfoTargetDate]" : "isChange",
			"click [data-menu-statisticsSearchInfo]" : "selectMenu"
		},
		initialize : function() {
			//console.log("This view has been initialized.");
			this.listenTo(this.model, "change", this.render);
			this.listenTo(this.model, "reset", this.render);
		},
		render : function() {
			//console.log("statisticsSearchInfoView render")
			var data = this.model.toJSON();
			data.getTargetDate = function() {
				return moment(data.startLocalDateTime).format(data.getMomentDateFormat());
			}
			this.$el
				.html(Mustache.render(this.template, data))
				.find("input[name=statisticsSearchInfoTargetDate]").datepicker({ format : data.getDatepickerDateFormat(), language: "ko", minViewMode : 1, autoclose : true })
			return this;
		},
		selectStatisticsSearchInfo : function(chronoUnit, targetLocalDate) {
			this.model.fetch({
				reset : true,
				data : $.param({
					chronoUnit : chronoUnit,
					targetLocalDate : targetLocalDate
				})
			});
		},
		isChange : function() {
			//console.log("statisticsSearchInfoView isChange");
			var targetDate = this.$el.find("input[name=statisticsSearchInfoTargetDate]").val();
			var data = this.model.toJSON();
			if (targetDate != moment(this.model.get("startLocalDateTime")).format(data.getMomentDateFormat())) {
				// 변경처리
				var chronoUnit = "YEARS";
				chronoUnit = data.chronoUnit;
				var targetLocalDate = moment(targetDate).add(this.model.get("baseDate") - 1, "days").format("YYYY-MM-DD");
				this.selectStatisticsSearchInfo(chronoUnit, targetLocalDate);
			}
		},
		selectMenu : function(event) {
			event.preventDefault();
			var menu = $(event.currentTarget).attr("data-menu-statisticsSearchInfo");
			var targetDate = this.$el.find("input[name=statisticsSearchInfoTargetDate]").val();
			var data = this.model.toJSON();
			var targetLocalDateMoment = moment(targetDate).add(this.model.get("baseDate") - 1, "days");
			if (menu == "prevDate") {
				targetLocalDateMoment.subtract(1, data.getMomentManipulateKey());
			} else if (menu == "nextDate") {
				targetLocalDateMoment.add(1, data.getMomentManipulateKey());
			}
			this.selectStatisticsSearchInfo(data.chronoUnit, targetLocalDateMoment.format("YYYY-MM-DD"));
		}
	});
});
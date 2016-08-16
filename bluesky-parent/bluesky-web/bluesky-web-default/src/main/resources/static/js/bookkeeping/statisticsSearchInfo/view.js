$(document).ready(function() {
	
	$.StatisticsSearchInfoView = Backbone.View.extend({
		el : "[data-menu-area=statisticsSearchInfo]",
		template : $("#template-statisticsSearchInfo-view").html(),
		events : {
			"change input[name=statisticsSearchInfoTargetDate]" : "isChange",
			"click [data-menu-statisticsSearchInfo]" : "selectMenu",
			"click [data-menu-selectStatisticsSearchInfoChronoUnit]" : "selectStatisticsSearchInfoChronoUnit"
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
				return moment(data.startZonedDateTime).format(data.getMomentDateFormat());
			}
			data.isChronoUnitYears = function() {
				return data.chronoUnit === "YEARS";
			}
			data.isChronoUnitMonths = function() {
				return data.chronoUnit === "MONTHS";
			}
			this.$el
				.html(Mustache.render(this.template, data))
				.find("input[name=statisticsSearchInfoTargetDate]").datepicker({ format : data.getDatepickerDateFormat(), language: "ko", minViewMode : data.getMomentManipulateKey(), autoclose : true }).end();
			return this;
		},
		selectStatisticsSearchInfo : function(chronoUnit, targetLocalDate) {
			//console.log("StatisticsSearchInfoView selectStatisticsSearchInfo", chronoUnit, targetLocalDate);
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
			var targetChronoUnit = this.$el.find("[data-menu-selectStatisticsSearchInfoChronoUnit].active").attr("data-menu-selectStatisticsSearchInfoChronoUnit");
			var data = this.model.toJSON();
			
			if (targetDate !== moment(this.model.get("startZonedDateTime")).format(data.getMomentDateFormat()) || data.chronoUnit !== targetChronoUnit) {
				// 변경처리
				this.selectStatisticsSearchInfo(targetChronoUnit, data.targetLocalDate);
			}
		},
		selectMenu : function(event) {
			//console.log("StatisticsSearchInfoView selectMenu");
			event.preventDefault();
			var menu = $(event.currentTarget).attr("data-menu-statisticsSearchInfo");
			var targetDate = this.$el.find("input[name=statisticsSearchInfoTargetDate]").val();
			var data = this.model.toJSON();
			var targetLocalDateMoment = moment(targetDate).add(this.model.get("baseDate") - 1, "days");
			if (menu === "prevDate") {
				targetLocalDateMoment.subtract(1, data.getMomentManipulateKey());
			} else if (menu === "nextDate") {
				targetLocalDateMoment.add(1, data.getMomentManipulateKey());
			}
			this.selectStatisticsSearchInfo(data.chronoUnit, targetLocalDateMoment.format("YYYY-MM-DD"));
		},
		selectStatisticsSearchInfoChronoUnit : function(event) {
			// 버튼 활성화 처리
			this.$el.find("[data-menu-selectStatisticsSearchInfoChronoUnit]").removeClass("active btn-info");
			$(event.currentTarget).addClass("active btn-info");
			this.isChange();
		}
	});
});
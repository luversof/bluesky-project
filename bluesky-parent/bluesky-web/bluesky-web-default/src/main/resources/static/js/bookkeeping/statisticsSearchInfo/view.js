$(document).ready(function() {
	
	$.StatisticsSearchInfoView = Backbone.View.extend({
		el : "[data-menu-area=statisticsSearchInfo]",
		template : $("#template-statisticsSearchInfo-view").html(),
		events : {
			"change input[name=statisticsSearchInfoTargetMonth]" : "isChange",
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
			data.getTargetMonth = function() {
				return moment(data.startLocalDateTime).format("YYYY-MM");
			}
			this.$el
				.html(Mustache.render(this.template, data))
				.find("input[name=statisticsSearchInfoTargetMonth]").datepicker({ format : "yyyy-mm", language: "ko", minViewMode : 1, autoclose : true })
			return this;
		},
		selectStatisticsSearchInfo : function(targetLocalDate) {
			this.model.fetch({
				reset : true,
				data : $.param({
					targetLocalDate : targetLocalDate
				})
			});
		},
		isChange : function() {
			//console.log("statisticsSearchInfoView isChange");
			var targetMonth = this.$el.find("input[name=statisticsSearchInfoTargetMonth]").val();
			if (targetMonth != moment(this.model.get("startLocalDateTime")).format("YYYY-MM")) {
				// 변경처리
				var targetLocalDate = moment(targetMonth).add(this.model.get("baseDate") - 1, "days").format("YYYY-MM-DD");
				this.selectStatisticsSearchInfo(targetLocalDate);
			}
		},
		selectMenu : function(event) {
			event.preventDefault();
			var menu = $(event.currentTarget).attr("data-menu-statisticsSearchInfo");
			var targetMonth = this.$el.find("input[name=statisticsSearchInfoTargetMonth]").val();
			var targetLocalDateMoment = moment(targetMonth).add(this.model.get("baseDate") - 1, "days");
			if (menu == "prevMonth") {
				targetLocalDateMoment.subtract(1, "months");
			} else if (menu == "nextMonth") {
				targetLocalDateMoment.add(1, "months");
			}
			this.selectStatisticsSearchInfo(targetLocalDateMoment.format("YYYY-MM-DD"));
		}
	});
});
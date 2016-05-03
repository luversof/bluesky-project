$(document).ready(function() {
	
	$.EntrySearchInfoView = Backbone.View.extend({
		el : "<div>",
		template : $("#template-entrySearchInfo-view").html(),
		events : {
			"change input[name=entrySearchInfoTargetMonth]" : "isChange"
		},
		initialize : function() {
//			console.log("This view has been initialized.");
			this.listenTo(this.model, "change", this.render);
		},
		render : function() {
			console.log("EntrySearchInfoView render")
			var data = this.model.toJSON();
			data.getTargetMonth = function() {
				return moment(data.startDateTime).format("YYYY-MM");
			}
			this.$el.html(Mustache.render(this.template, data));
			
			this.$el
				.find("input[name=entrySearchInfoTargetMonth]").datepicker({ format : "yyyy-mm", language: "ko", minViewMode : 1 })
			return this;
		},
		isChange : function() {
			console.log("EntrySearchInfoView isChange");
			var targetMonth = this.$el.find("input[name=entrySearchInfoTargetMonth]").val();
			if (targetMonth == moment(this.model.get("startDateTime")).format("YYYY-MM")) {
				
			} else {
				console.log("변경처리");
				// 변경처리
				var targetLocalDate = moment(targetMonth).add(this.model.get("baseDate") - 1, "days").format("YYYY-MM-DD");
				this.model.fetch({
					data : $.param({
						targetLocalDate : targetLocalDate
					})
				});
			}
		}
		
	});
});
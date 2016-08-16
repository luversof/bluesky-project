$.Entry = Backbone.Model.extend({
	defaults : {
		bookkeeping : { id : $.bookkeepingId },
		entryType : null,	// 입력 유형 선택을 위한 변수
		entryGroup : null,
		debitAsset : null,
		creditAsset : null,
		amount : 0,
		entryDate : null,
		memo : null
	},
	initialize : function() {
		this.on("invalid", function(model, error) {
			alert(error);
		});
	},
	validate : function(attrs, options) {
		console.log("validate : ", attrs, options);
		if (attrs.amount === 0) {
			return "금액을 입력하세요.";
		}
		// 이건 필수가 아니어도 될거 같음
		if (attrs.memo === null || attrs.memo === "") {
			return "내용을 입력하세요.";
		}
		if (attrs.entryDate === null || attrs.entryDate === "" || attrs.entryDate === "Invalid date") {
			return "날짜를 입력하세요.";
		}
	}
});
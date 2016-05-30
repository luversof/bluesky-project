/**
 * StatisticsCollection은 Entry Model을 기준으로 처리
 */
$.StatisticsCollection = Backbone.Collection.extend({
	model : $.Entry,
	url : "/bookkeeping/" + $.bookkeepingId + "/statistics",
//	comparator: "entryDate",
	initialize : function() {
		//console.log("AssetCollection initialized.");
	}
});
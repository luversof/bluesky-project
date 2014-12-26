//타임라인

//1. 확대 축소, 이동이 가능한 연대 표를 만들자.
// 디자인은 변경하기 쉽게
var Timeline = function() {
	var timlineArea = $(".timeline");
	return {
		makeTimelineArea : function () {
			console.log("test");
		},
		init : function() {
			this.makeTimelineArea();
		}
	};
};
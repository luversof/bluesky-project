$(document).ready(function() {
	
	//var svg = d3.select("body")
	var svg = d3.select("#statisticsChartArea")
		.append("svg")
		.append("g")
	
	svg.append("g").attr("class", "slices");
	svg.append("g").attr("class", "labels");
	svg.append("g").attr("class", "lines");
	
	var width = 960,
	height = 450,
	radius = Math.min(width, height) / 2;
	
	var pie = d3.layout.pie()
	.sort(null)
	.value(function(d) {
		return d.value;
	});
	
	var arc = d3.svg.arc()
	.outerRadius(radius * 0.8)
	.innerRadius(radius * 0.4);
	
	var outerArc = d3.svg.arc()
	.innerRadius(radius * 0.9)
	.outerRadius(radius * 0.9);
	
	svg.attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");
	
	var key = function(d){ return d.data.label; };
	
	{
	
		var entryType = "CREDIT";
		
		//0. 대상 원본 데이터
		var statisticsArray = [{"amount":11212467,"entryGroup":{"id":2,"name":"이자","entryType":"CREDIT","bookkeeping":{"id":1,"name":"이자","userId":1,"baseDate":4}},"entryType":"CREDIT"},{"amount":123,"entryGroup":{"id":4,"name":"사회생활","entryType":"DEBIT","bookkeeping":{"id":1,"name":"사회생활","userId":1,"baseDate":4}},"entryType":"DEBIT"},{"amount":123524,"entryGroup":{"id":5,"name":"문화","entryType":"DEBIT","bookkeeping":{"id":1,"name":"문화","userId":1,"baseDate":4}},"entryType":"DEBIT"},{"amount":124357,"entryGroup":{"id":6,"name":"기타","entryType":"CREDIT","bookkeeping":{"id":1,"name":"기타","userId":1,"baseDate":4}},"entryType":"CREDIT"},{"amount":1312312,"entryGroup":{"id":7,"name":"상여금","entryType":"CREDIT","bookkeeping":{"id":1,"name":"상여금","userId":1,"baseDate":4}},"entryType":"CREDIT"},{"amount":123232,"entryGroup":{"id":8,"name":"교통비","entryType":"DEBIT","bookkeeping":{"id":1,"name":"교통비","userId":1,"baseDate":4}},"entryType":"DEBIT"},{"amount":12325435,"entryGroup":{"id":9,"name":"월급","entryType":"CREDIT","bookkeeping":{"id":1,"name":"월급","userId":1,"baseDate":4}},"entryType":"CREDIT"}];
		console.log("statisticsArray : ", statisticsArray);
		
		//1. 수입의 경우만 테스트를 해보자
		var filteredStatisticsArray = statisticsArray.filter(function(statistics) {
			return statistics.entryGroup.entryType == entryType;
		})
		console.log("filteredStatisticsArray : ", filteredStatisticsArray);
		
		//1-1. color 목록 계산 (이렇게 하는건 무슨 이유때문이지?)
		var color = d3.scale.category20()
			.domain(filteredStatisticsArray.map(function(statistics) {
				return statistics.entryGroup.name;
			}));
		console.log("color : ", color);
		console.log("color 테스트 : ", color("월급"));
		
		//1-2. 전체 총 합 계산
		var statisticsAmountSum = d3.sum(filteredStatisticsArray, function(statistics) { return statistics.amount });
		console.log("statisticsAmountSum : ", statisticsAmountSum);
		
		//1-3. 표시할 데이터 배열 생성
		targetStatistics = filteredStatisticsArray.map(function(statistics) {
			var percent = ((statistics.amount * 100) / statisticsAmountSum).toFixed(2);
			return { label : statistics.entryGroup.name + " " + percent + "%", value : statistics.amount }
		});
		
		
		d3.select(".randomize")
		.on("click", function(){
			change(targetStatistics, color);
		});
	}
	
	function mergeWithFirstEqualZero(first, second){
		var secondSet = d3.set(); second.forEach(function(d) { secondSet.add(d.label); });
		
		var onlyFirst = first
		.filter(function(d){ return !secondSet.has(d.label) })
		.map(function(d) { return {label: d.label, value: 0}; });
		return d3.merge([ second, onlyFirst ])
		.sort(function(a,b) {
			return d3.ascending(a.label, b.label);
		});
	}
	
	function change(data, color) {
		var duration = 150;
		var data0 = svg.select(".slices").selectAll("path.slice")
		.data().map(function(d) { return d.data });
		if (data0.length == 0) data0 = data;
		var was = mergeWithFirstEqualZero(data, data0);
		var is = mergeWithFirstEqualZero(data0, data);
		
		/* ------- SLICE ARCS -------*/
		
		var slice = svg.select(".slices").selectAll("path.slice")
		.data(pie(was), key);
		
		slice.enter()
		.insert("path")
		.attr("class", "slice")
		.style("fill", function(d) { return color(d.data.label); })
		.each(function(d) {
			this._current = d;
		});
		
		slice = svg.select(".slices").selectAll("path.slice")
		.data(pie(is), key);
		
		slice		
		.transition().duration(duration)
		.attrTween("d", function(d) {
			var interpolate = d3.interpolate(this._current, d);
			var _this = this;
			return function(t) {
				_this._current = interpolate(t);
				return arc(_this._current);
			};
		});
		
		slice = svg.select(".slices").selectAll("path.slice")
		.data(pie(data), key);
		
		slice
		.exit().transition().delay(duration).duration(0)
		.remove();
		
		/* ------- TEXT LABELS -------*/
		
		var text = svg.select(".labels").selectAll("text")
		.data(pie(was), key);
		
		text.enter()
		.append("text")
		.attr("dy", ".35em")
		.style("opacity", 0)
		.text(function(d) {
			return d.data.label;
		})
		.each(function(d) {
			this._current = d;
		});
		
		function midAngle(d){
			return d.startAngle + (d.endAngle - d.startAngle)/2;
		}
		
		text = svg.select(".labels").selectAll("text")
		.data(pie(is), key);
		
		text.transition().duration(duration)
		.style("opacity", function(d) {
			return d.data.value == 0 ? 0 : 1;
		})
		.attrTween("transform", function(d) {
			var interpolate = d3.interpolate(this._current, d);
			var _this = this;
			return function(t) {
				var d2 = interpolate(t);
				_this._current = d2;
				var pos = outerArc.centroid(d2);
				pos[0] = radius * (midAngle(d2) < Math.PI ? 1 : -1);
				return "translate("+ pos +")";
			};
		})
		.styleTween("text-anchor", function(d){
			var interpolate = d3.interpolate(this._current, d);
			return function(t) {
				var d2 = interpolate(t);
				return midAngle(d2) < Math.PI ? "start":"end";
			};
		});
		
		text = svg.select(".labels").selectAll("text")
		.data(pie(data), key);
		
		text
		.exit().transition().delay(duration)
		.remove();
		
		/* ------- SLICE TO TEXT POLYLINES -------*/
		
		var polyline = svg.select(".lines").selectAll("polyline")
		.data(pie(was), key);
		
		polyline.enter()
		.append("polyline")
		.style("opacity", 0)
		.each(function(d) {
			this._current = d;
		});
		
		polyline = svg.select(".lines").selectAll("polyline")
		.data(pie(is), key);
		
		polyline.transition().duration(duration)
		.style("opacity", function(d) {
			return d.data.value == 0 ? 0 : .5;
		})
		.attrTween("points", function(d){
			this._current = this._current;
			var interpolate = d3.interpolate(this._current, d);
			var _this = this;
			return function(t) {
				var d2 = interpolate(t);
				_this._current = d2;
				var pos = outerArc.centroid(d2);
				pos[0] = radius * 0.95 * (midAngle(d2) < Math.PI ? 1 : -1);
				return [arc.centroid(d2), outerArc.centroid(d2), pos];
			};			
		});
		
		polyline = svg.select(".lines").selectAll("polyline")
		.data(pie(data), key);
		
		polyline
		.exit().transition().delay(duration)
		.remove();
	};
});

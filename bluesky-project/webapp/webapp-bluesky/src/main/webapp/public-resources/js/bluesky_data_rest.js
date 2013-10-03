String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

var View = function(options) {
	this.template = options.template;
	this.target = options.target;
	
	this.render = function(data) {
		console.log("render");
		console.log(this.target);
		this.target.append(Mustache.render(this.template, data));
	};
};

/**
 * action에 대해 선언한 클래스
 * 모든 액션은 Controller로 집중됨
 */
var Controller = function(options) {
	this.url = options.url;
	this.id = options.id;
	this.dataKey = options.dataKey;
	this.view = options.view;
	this.ajax = { dataType : "json" };
	this.initialize = function() {
		console.log("initialize");
		console.log(this.url);
		if (!this.url) {
			throw new Error("url empty");
		}
		if (!this.id) {
			throw new Error("id empty");
		}
	}; 
	

	this.get = function(data) {
		if (!data[this.id]) {
			throw new Error("id empty");
		}
		var controller = this;
		console.log("get method url : " + this.url + "/" + data[this.id]);
		return $.ajax({
			url : this.url + "/" + data[this.id],
			type : "get",
			dataType : this.ajax.dataType,
			success : function(data) {
				console.log("success");
				controller.setData(data);
			}
		});
	};
	
	this.getDataList = function() {
		var list = this.view.target.data(this.dataKey);
		if (list == null) {
			list = new Array();
		}
		return list;
	};
	this.setData = function(data) {
		var list = this.getDataList();
		list.push(data);
		this.view.target.data(this.dataKey, list);
		this.view.render(data);
		return list;
	};
	this.getData = function(id) {
		var list = this.getDataList();
		for (key in list) {
			if (list[key][this.id] == id) {
				return list[key];
			}
		}
	};
	
	this.save = function() {
		return $.ajax({
			url : this.url,
			type : "post",
			data : this.model.data,
			success : function() {
				console.log("asdg");
				console.log();
			}
		});
	};
	
	this.modify = function() {
		
	};
	
	this.remove = function() {
		
	};
};

var Model = function(options) {
	this.controller = options.controller;
	this.get = function() {
		console.log("model get");
		this.controller.get(this);
	};
};



$(document).ready(function() {
	/**
	 * 1. view 설정
	 */
	var view = new View({
		template : $("#entry-template").text(),
		target : $(".table.table-hover tbody")
	});
	
	/**
	 * 2. controller 설정
	 */
	var controller = new Controller({
		url : "/user/1/bookkeeping/entry",
		id : "id",
		dataKey : "model",
		view : view
	});
	var model = new Model({controller : controller});
	
	//var data = new Model({}, {controller:controller});
	var data = $.extend({id : 49}, model);
	data.get();
});
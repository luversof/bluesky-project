String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

var View = function() {
	this.template;
	
	this.render = function() {
		
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
			throw new Error("url must defined.");
		}
	}; 
	

	this.get = function(data) {
		console.log(data);
		if (!data[this.id]) {
			throw new Error("id empty");
		}
		var target = this;
		console.log("get method" + this.url + "/" + data[this.id]);
		return $.ajax({
			url : this.url + "/" + data[this.id],
			type : "get",
			dataType : this.ajax.dataType,
			success : function(data) {
				console.log("success");
				console.log(data);
				$(document).data(target.dataKey, data);
				target.view.render($(document).data(target.dataKey));
			}
		});
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
		controller.get(this);
	};
};



/**
 * 2. view 설정
 */
var view = {};
$.extend(view, new View(), {
	
});
/**
 * 3. controller 설정
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

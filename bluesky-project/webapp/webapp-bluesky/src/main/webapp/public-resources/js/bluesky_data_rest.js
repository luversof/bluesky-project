String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

var View = function(options) {
	this.template,this.target;
	this.render = function(model) {
		console.log("[view] render...");
		this.target.append($(Mustache.render(this.template, model.data)).attr("data-id", model.getId()));
	};
	var _initialize = function(obj) {
		console.log("[view] _initialize...");
		obj.initialize();
	};
	this.initialize = function() {};
	
	for (key in options) this[key] = options[key];
	_initialize(this);
};

/**
 * action에 대해 선언한 클래스
 * 모든 액션은 Controller로 집중됨
 */
var Controller = function(options) {
	this.url,this.id,this.dataKey,this.view;
	this.ajax = { dataType : "json", contentType : "application/json" };
	var _initialize = function(obj) {
		console.log("[controller] _initialize...");
		if (!obj.url) {
			throw new Error("[controller] _initialize error : url empty");
		}
		if (!obj.id) {
			throw new Error("[controller] _initialize error : id empty");
		}
		obj.view.target.empty();
		obj.initialize();
	};
	this.initialize = function() {};
	this.getUrl = function() {return this.url;};
	this.events = function() {};
	var _initializeEvents = function(obj) {
		console.log("[controller] _initializeEvents...");
		for (key in obj.events) {
			for (event in obj.events[key]) {
				$(obj.view.target).on(event, key, { controller : obj }, obj[obj.events[key][event]]);
			}
		}
	};
	
	this.getModelList = function() {
		var obj = this;
		console.log("[controller] get");
		console.log("	url : " + this.getUrl());
		return $.ajax({
			url : this.url,
			type : "get",
			dataType : this.ajax.dataType,
			success : function(data) {
				for (key in data) {
					obj.setModelData(new Model(data[key], {controller : obj}));
				}
			}
		});
	};

	this.getModel = function(model) {
		if (!model.getId()) {
			throw new Error("[controller] get error : id empty");
		}
		var obj = this;
		console.log("[controller] get");
		console.log("	url : " + this.getUrl() + "/" + model.getId());
		return $.ajax({
			url : this.url + "/" + model.getId(),
			type : "get",
			dataType : this.ajax.dataType,
			success : function(data) {
				$.extend(model.data, data);
				obj.setModelData(model);
			}
		});
	};
	
	this.saveModel = function(model) {
		console.log("[controller] save");
		return $.ajax({
			url : this.url,
			type : "post",
			data : JSON.stringify(model.data),
			dataType : this.ajax.dataType,
			contentType : this.ajax.contentType,
			success : function() {
				console.log("asdg");
			}
		});
	};
	
	this.modify = function() {
		
	};
	
	this.remove = function() {
		
	};
	
	this.getModelListData = function() {
		var list = this.view.target.data(this.dataKey);
		if (list == null) {
			list = new Array();
		}
		return list;
	};
	this.setModelData = function(model) {
		console.log("[controller] setData");
		var list = this.getModelListData();
		list.push(model);
		this.view.target.data(this.dataKey, list);
		this.view.render(model);
		return list;
	};
	this.getModelData = function(id) {
		var list = this.getModelListData();
		for (key in list) {
			if (list[key][this.id] == id) {
				return list[key];
			}
		}
	};
	
	for (key in options) this[key] = options[key];
	_initialize(this);
	_initializeEvents(this);
};

var Model = function(data, options) {
	this.data = data;
	this.controller;
	this.getId = function() {
		console.log("[model] getId");
		return this.data[this.controller.id];
	};
	this.get = function() {
		console.log("[model] get -> controller.getModel(model)");
		this.controller.getModel(this);
	};
	this.save = function() {
		console.log("[model] save -> controller.saveModel(model)");
		this.controller.saveModel(this);
	};
	var _initialize = function(obj) {
		console.log("[model] _initialize...");
		obj.initialize();
		
		if (!obj.data[obj.controller.id]) obj.data[obj.controller.id] = null;
	};
	this.initialize = function() {};
	
	for (key in options) this[key] = options[key];
	_initialize(this);
};
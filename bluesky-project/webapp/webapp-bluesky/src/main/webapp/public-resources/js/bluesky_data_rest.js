String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

var Model = function(data, options) {
	this.data = data;
	this.controller;
	this.getId = function() {
		console.debug("[model] getId");
		return this.data[this.controller.id];
	};
	this.get = function() {
		console.debug("[model] get -> controller.getModel(model)");
		return this.controller.getModel(this);
	};
	this.add = function() {
		console.debug("[model] add -> controller.addModel(model)");
		return this.controller.addModel(this);
	};
	var _initialize = function(obj) {
		console.debug("[model] _initialize...");
		obj.initialize();
		if (!obj.data[obj.controller.id]) obj.data[obj.controller.id] = null;
	};
	this.initialize = function() {};
	for (key in options) this[key] = options[key];
	_initialize(this);
};

var View = function(options) {
	this.template,this.target;
	this.dataIdKey = "data-id";
	this.render = function(model) {
		console.debug("[view] render...");
		var renderedTemplate = $(Mustache.render(this.template, model.data)).attr(this.dataIdKey, model.getId());
		var renderTarget = this.target.find("[" + this.dataIdKey + "=" + model.getId() + "]");
		if (renderTarget.length == 0) {
			this.target.append(renderedTemplate);
		} else {
			renderTarget.replaceWith(renderedTemplate);
		}
	};
	var _initialize = function(obj) {
		console.debug("[view] _initialize...");
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
		console.debug("[controller] _initialize...");
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
	this.events = {};
	this.externalEvents = {};
	
	var _initializeEvents = function(obj) {
		console.debug("[controller] _initializeEvents...");
		var _addEvent = function(obj, target, event, eventMethod) {
			console.debug("[controller] _addEvent target : %s, event : %s, eventMethod : %s", target, event, eventMethod);
			$(obj.view.target).on(event, target, { controller : obj }, eventMethod);
		};
		for (target in obj.events) {
			for (event in obj.events[target]) {
				if (typeof obj.events[target][event] == "object") {
					for (eventListPart in obj.events[target][event]) {
						_addEvent(obj, target, event, obj[obj.events[target][event][eventListPart]]);
					}
				} else {
					_addEvent(obj, target, event, obj[obj.events[target][event]]);
				}
			}
		}
	};
	var _initializeExternalEvents = function(obj) {
		console.debug("[controller] _initializeExternalEvents...");
		var _addEvent = function(obj, target, event, eventMethod) {
			console.debug("[controller] _addEvent target : %s, event : %s, eventMethod : %s", target, event, eventMethod);
			$(target).on(event, null, { controller : obj }, eventMethod);
		};
		for (target in obj.externalEvents) {
			for (event in obj.externalEvents[target]) {
				if (typeof obj.externalEvents[target][event] == "object") {
					for (eventListPart in obj.externalEvents[target][event]) {
						_addEvent(obj, target, event, obj[obj.externalEvents[target][event][eventListPart]]);
					}
				} else {
					_addEvent(obj, target, event, obj[obj.externalEvents[target][event]]);
				}
			}
		}
	};
	this.getModelList = function() {
		var obj = this;
		console.debug("[controller] getModelList url : %s", this.getUrl());
		return $.ajax({
			url : this.url,
			type : "get",
			dataType : this.ajax.dataType,
			success : function(data) {
				for (key in data) {
					obj.addSavedModel(new Model(data[key], {controller : obj}));
				}
			}
		});
	};
	this.getModel = function(model) {
		if (!model.getId()) {
			throw new Error("[controller] getModel error : id empty");
		}
		var obj = this;
		console.debug("[controller] getModel url : %s/%s", this.getUrl(), model.getId());
		return $.ajax({
			url : this.url + "/" + model.getId(),
			type : "get",
			dataType : this.ajax.dataType,
			success : function(data) {
				$.extend(model.data, data);
				obj.addSavedModel(model);
			}
		});
	};
	this.addModel = function(model) {
		console.debug("[controller] addModel");
		var obj = this;
		return $.ajax({
			url : this.url,
			type : "post",
			data : JSON.stringify(model.data),
			dataType : this.ajax.dataType,
			contentType : this.ajax.contentType,
			success : function(data) {
				$.extend(model.data, data);
				obj.addSavedModel(model);
			}
		});
	};
	this.modifyModel = function(model) {
		console.debug("[controller] modifyModel");
		return $.ajax({
			url : this.url + "/" + model.getId(),
			type : "put",
			data : JSON.stringify(model.data),
			dataType : this.ajax.dataType,
			contentType : this.ajax.contentType,
			success : function(data) {
				$.extend(model.data, data);
			}
		});
	};
	this.removeModel = function(model) {
		console.debug("[controller] removeModel");
		var obj = this;
		return $.ajax({
			url : this.url + "/" + model.getId(),
			type : "delete",
			dataType : this.ajax.dataType,
			contentType : this.ajax.contentType,
			success : function(data) {
				obj.removeSavedModel(model.getId());
				console.log(obj.getSavedModelList());
			}
		});
	};
	
	this.getSavedModelList = function() {
		var list = this.view.target.data(this.dataKey);
		if (list == null) {
			list = new Array();
		}
		return list;
	};
	this.addSavedModel = function(model) {
		console.debug("[controller] addSavedModel");
		var list = this.getSavedModelList();
		list.push(model);
		this.view.target.data(this.dataKey, list);
		this.view.render(model);
		return list;
	};
	this.getSavedModel = function(id) {
		var list = this.getSavedModelList();
		console.debug("[controller] getSavedModel");
		for (key in list) {
			if (list[key].data[this.id] == id) {
				return list[key];
			}
		}
	},
	this.removeSavedModel = function(id) {
		var list = this.getSavedModelList();
		console.debug("[controller] getSavedModel");
		for (key in list) {
			if (list[key].data[this.id] == id) {
				list.splice(key, 1);
				break;
			}
		}
	};
	for (key in options) this[key] = options[key];
	_initialize(this);
	_initializeEvents(this);
	_initializeExternalEvents(this);
};


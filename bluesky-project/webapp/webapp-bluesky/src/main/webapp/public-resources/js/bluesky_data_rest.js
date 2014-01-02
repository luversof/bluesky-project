/**
 * String format 적용<br />
 * var ex = "Hello {0}!, This is {1}"<br />
 * ex.format("World", "Test");<br />
 * => Hello World!, This is Test
 * @returns
 */
String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

Array.prototype.sortData = function(key, isDescending) {
	if (!isDescending) isDescending = false;
	var i = isDescending == true ? -1 : 1;
	this.sort(function(a, b) {
		return getValueFromObj(a, key) > getValueFromObj(b, key) ? i * 1 : i * -1; 
	});
};

/**
 * object에서 key에 대한 value return<br />
 * ex) obj = {a : { a1 : "a1val", ...}, b : {...}}<br />
 * key = "a.a1"<br />
 * getValueFromObj(obj, key) => "a1val" 
 */
function getValueFromObj(obj, key) {
	var keyArray = key.split(".");
	var retVal = obj;
	for (var i = 0 ; i < keyArray.length ; i++) {
		retVal = retVal[keyArray[i]];
	}
	return retVal;
}

var t = [
	{a : "1234", b : { b1 : "bag", b2 : "v32"}},
	{a : "3234", b : { b1 : "cehg", b2 : "v22"}},
	{a : "2342", b : { b1 : "asd", b2 : "v12"}}];
//t.sortData("b.b1");
//console.log(t);


/**
 * original data와 ui 추출 데이터 사이 변경 여부 확인
 * @param original
 * @returns {Boolean}
 */
String.prototype.isChanged = function(original) {
	//console.log("====this : %s, original : %s, typeof this : %s, typeof original : %s", this.valueOf(), original, typeof this, typeof original);
	if (typeof original == "string") {
		//console.log("string diff : %s", this != original);
		return this != original;
	} else if (typeof original == "boolean") {
		//console.log("boolean diff : %s, typeof (this) : %s", eval(this.valueOf()) != original, typeof new Boolean(this));
		return eval(this.valueOf()) != original;
	} else {
		//console.log("else diff : %s, typeof (this) : %s", eval(this) != original, typeof eval(this));
		return eval(this) != original;
	}
};

/**
 * Object에 대한 관리 클래스
 */
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
	this.modify = function() {
		console.debug("[model] modify -> controller.modifyModel(model)");
		return this.controller.modifyModel(this);
	};
	this.remove = function() {
		console.debug("[model] remove -> controller.removeModel(model)");
		return this.controller.removeModel(this);
	};
	var _initialize = function(obj) {
		console.debug("[model] _initialize...");
		obj.initialize();
		if (!obj.data[obj.controller.id]) obj.data[obj.controller.id] = null;
	};
	this.initialize = function() {};
	for (key in options) {
		this[key] = options[key];
	}
	_initialize(this);
};

/**
 * display 관련 클래스
 */
var View = function(options) {
	this.template,this.target;
	this.dataIdKey = "data-id";
	
	/**
	 * model을 view 에 추가, 기존에 있는 경우 갱신 처리
	 */
	this.addView = function(model) {
		console.debug("[view] addView...");
		var renderedTemplate = $(Mustache.render(this.template, model.data)).attr(this.dataIdKey, model.getId());
		// 추가 대상이 이미 view에 있는 경우 replace 처리
		var renderTarget = this.getView(model.getId());
		if (renderTarget.length == 0) {
			// 순서에 따라 위치 변경 처리 필요
			this.target.append(renderedTemplate);
		} else {
			renderTarget.replaceWith(renderedTemplate);
		}
		return renderTarget;
	};
	this.getView = function(id) {
		return this.target.find("[" + this.dataIdKey + "=" + id + "]");
	};
	this.removeView = function(id) {
		this.getView(id).remove();
	};
	var _initialize = function(obj) {
		console.debug("[view] _initialize...");
		obj.initialize();
	};
	this.initialize = function() {};
	for (key in options) {
		this[key] = options[key];
	}
	_initialize(this);
};

/**
 * action에 대해 선언한 클래스 <br />
 * ajax model 호출 관련 메소드
 * 	 getModelList, getModel, addModel, modifyModel, removeModel
 * 저장한 model 호출 관련 메소드
 * model 관련 메소드 호출 시 view에 대한 관련 method도 같이 동작 수행하도록 처리
 * getSavedModelList, getSavedModel, addSavedModel, removeSavedModel
 */
var Controller = function(options) {
	this.url,this.view;
	this.id = "id";
	this.savedModelKey = "savedModel";
	this.sortKey = "name";
	this.sortIsDescending = false;
	
	this.ajaxConfig= { dataType : "json", contentType : "application/json" };
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
			//console.debug("[controller] _addEvent target : %s, event : %s, eventMethod : %s", target, event, eventMethod);
			$(obj.view.target).on(event, target, { controller : obj }, eventMethod);
		};
		for (target in obj.events) {
			for (event in obj.events[target]) {
				if (typeof obj.events[target][event] == "object") {
					for (var i = 0 ; i < obj.events[target][event].length ; i++) {
						_addEvent(obj, target, event, obj[obj.events[target][event][i]]);
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
			//console.debug("[controller] _addEvent target : %s, event : %s, eventMethod : %s", target, event, eventMethod);
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
			url : this.getUrl(),
			type : "get",
			dataType : this.ajaxConfig.dataType,
			success : function(data) {
				for (var i = 0 ; i < data.length ; i++) {
					obj.addSavedModel(new Model(data[i], {controller : obj}));
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
			url : this.getUrl() + "/" + model.getId(),
			type : "get",
			dataType : this.ajaxConfig.dataType,
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
			url : this.getUrl(),
			type : "post",
			data : JSON.stringify(model.data),
			dataType : this.ajaxConfig.dataType,
			contentType : this.ajaxConfig.contentType,
			success : function(data) {
				$.extend(model.data, data);
				obj.addSavedModel(model);
			}
		});
	};
	this.modifyModel = function(model) {
		console.debug("[controller] modifyModel");
		return $.ajax({
			url : this.getUrl() + "/" + model.getId(),
			type : "put",
			data : JSON.stringify(model.data),
			dataType : this.ajaxConfig.dataType,
			contentType : this.ajaxConfig.contentType,
			success : function(data) {
				$.extend(model.data, data);
			}
		});
	};
	this.removeModel = function(model) {
		console.debug("[controller] removeModel");
		var obj = this;
		return $.ajax({
			url : this.getUrl() + "/" + model.getId(),
			type : "delete",
			dataType : this.ajaxConfig.dataType,
			contentType : this.ajaxConfig.contentType,
			success : function(data) {
				obj.removeSavedModel(model.getId());
				console.log(obj.getSavedModelList());
			}
		});
	};

	this.getSavedModelList = function() {
		var list = this.view.target.data(this.savedModelKey);
		if (list == null) {
			list = new Array();
		}
		return list;
	};
	this.addSavedModel = function(model) {
		console.debug("[controller] addSavedModel");
		var list = this.getSavedModelList();
		list.push(model);
		list.sortData("data." + this.sortKey, this.sortIsDescending);
		this.view.target.data(this.savedModelKey, list);
		this.view.addView(model);
		return list;
	};
	this.getSavedModel = function(id) {
		console.debug("[controller] getSavedModel");
		var list = this.getSavedModelList();
		for (var i = 0; i < list.length; i++) {
			if (list[i].data[this.id] == id) {
				return list[i];
			}
		}
	}, this.removeSavedModel = function(id) {
		console.debug("[controller] removeSavedModel");
		this.view.removeView(id);
		var list = this.getSavedModelList();
		for (var i = 0; i < list.length; i++) {
			if (list[i].data[this.id] == id) {
				list.splice(i, 1);
				break;
			}
		}
	};
	for (key in options) {
		this[key] = options[key];
	}
	_initialize(this);
	_initializeEvents(this);
	_initializeExternalEvents(this);
};
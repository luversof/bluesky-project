var orgConsole = console;
var console = {
	debug : function(message) {}
	, log : function(message) {
		orgConsole.log(message);
	}
};
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
	if (isDescending == undefined) isDescending = false;
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
console.log("d2s");
t.sortData("a", true);
console.log(t);


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
	var _self = this;
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
	var _initialize = function() {
		console.debug("[model] _initialize...");
		_self.initialize();
		if (!_self.data[_self.controller.id]) _self.data[_self.controller.id] = null;
	};
	this.initialize = function() {};
	for (key in options) {
		this[key] = options[key];
	}
	_initialize();
};

/**
 * display 관련 클래스
 */
var View = function(options) {
	var _self = this;
	this.template;
	this.target;	// 대상이 추가될 위치
	this.dataIdKey = "data-bluesky-rest-js-view-id";
	
	/**
	 * model을 view 에 추가, 기존에 있는 경우 갱신 처리
	 * 
	 * 템플릿을 해당 위치에 넣는다.
	 */
	this.add = function(model, prevModelId) {
		console.debug("[view] add...");
		var renderedTemplate = $(Mustache.render(this.template, model.data)).attr(this.dataIdKey, model.getId());
		// 추가 대상이 이미 view에 있는 경우 replace 처리
		var renderTarget = this.get(model.getId());
		if (renderTarget.length == 0) {
			// 순서에 따라 위치 변경 처리 필요
			if (prevModelId != undefined) {
				//console.log("prevModelIDDDDDDDDDDDDDDDD : " + prevModelId);
				//console.log("[{0}={1}]".format(this.dataIdKey, prevModelId));
				//console.log(this.target.find("[{0}={1}]".format(this.dataIdKey, prevModelId)).html());
				this.target.find("[{0}={1}]".format(this.dataIdKey, prevModelId)).after(renderedTemplate);
			} else {
				this.target.append(renderedTemplate);
			}
		} else {
			renderTarget.replaceWith(renderedTemplate);
		}
		return renderTarget;
	};
//	this.isExist = function(id) {
//		return this.get(id).length == 0;
//	};
	
	
	this.get = function(id) {
		return this.target.find("[{0}={1}]".format(this.dataIdKey, id));
	};
	this.remove = function(id) {
		this.get(id).remove();
	};
	var _initialize = function() {
		console.debug("[view] _initialize...");
		_self.initialize();
	};
	this.initialize = function() {};
	for (key in options) {
		this[key] = options[key];
	}
	_initialize();
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
	var _self = this;
	this.url;
	this.view;
	this.id = "id";
	this.savedModelKey = "savedModel";
	this.sortKey = "name";
	this.sortIsDescending = true;
	
	this.ajaxConfig= { dataType : "json", contentType : "application/json" };
	var _initialize = function() {
		console.debug("[controller] _initialize...");
		if (!_self.url) {
			throw new Error("[controller] _initialize error : url empty");
		}
		if (!_self.id) {
			throw new Error("[controller] _initialize error : id empty");
		}
		_self.view.target.empty();
		_self.initialize();
	};
	this.initialize = function() {};
	this.getUrl = function() {return this.url;};
	this.events = {};
	this.externalEvents = {};
	
	var _initializeEvents = function() {
		console.debug("[controller] _initializeEvents...");
		var _addEvent = function(_self, target, event, eventMethod) {
			//console.debug("[controller] _addEvent target : %s, event : %s, eventMethod : %s", target, event, eventMethod);
			$(_self.view.target).on(event, target, { controller : _self }, eventMethod);
		};
		for (target in _self.events) {
			for (event in _self.events[target]) {
				if (typeof _self.events[target][event] == "object") {
					for (var i = 0 ; i < _self.events[target][event].length ; i++) {
						_addEvent(_self, target, event, _self[_self.events[target][event][i]]);
					}
				} else {
					_addEvent(_self, target, event, _self[_self.events[target][event]]);
				}
			}
		}
	};
	var _initializeExternalEvents = function() {
		console.debug("[controller] _initializeExternalEvents...");
		var _addEvent = function(_self, target, event, eventMethod) {
			//console.debug("[controller] _addEvent target : %s, event : %s, eventMethod : %s", target, event, eventMethod);
			$(target).on(event, null, { controller : _self }, eventMethod);
		};
		for (target in _self.externalEvents) {
			for (event in _self.externalEvents[target]) {
				if (typeof _self.externalEvents[target][event] == "object") {
					for (eventListPart in _self.externalEvents[target][event]) {
						_addEvent(_self, target, event, _self[_self.externalEvents[target][event][eventListPart]]);
					}
				} else {
					_addEvent(_self, target, event, _self[_self.externalEvents[target][event]]);
				}
			}
		}
	};
	this.getModelList = function() {
		console.debug("[controller] getModelList url : %s", this.getUrl());
		return $.ajax({
			url : this.getUrl(),
			type : "get",
			dataType : this.ajaxConfig.dataType,
			success : function(data) {
				for (var i = 0 ; i < data.length ; i++) {
					_self.addSavedModel(new Model(data[i], {controller : _self}));
				}
			}
		});
	};
	this.getModel = function(model) {
		if (!model.getId()) {
			throw new Error("[controller] getModel error : id empty");
		}
		console.debug("[controller] getModel url : %s/%s", this.getUrl(), model.getId());
		return $.ajax({
			url : this.getUrl() + "/" + model.getId(),
			type : "get",
			dataType : this.ajaxConfig.dataType,
			success : function(data) {
				$.extend(model.data, data);
				_self.addSavedModel(model);
			}
		});
	};
	this.addModel = function(model) {
		console.debug("[controller] addModel");
		return $.ajax({
			url : this.getUrl(),
			type : "post",
			data : JSON.stringify(model.data),
			dataType : this.ajaxConfig.dataType,
			contentType : this.ajaxConfig.contentType,
			success : function(data) {
				$.extend(model.data, data);
				_self.addSavedModel(model);
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
		return $.ajax({
			url : this.getUrl() + "/" + model.getId(),
			type : "delete",
			dataType : this.ajaxConfig.dataType,
			contentType : this.ajaxConfig.contentType,
			success : function(data) {
				_self.removeSavedModel(model.getId());
				console.log(_self.getSavedModelList());
			}
		});
	};
	/**
	 * target에 저장된 modelList 호출
	 */
	this.getSavedModelList = function() {
		var list = this.view.target.data(this.savedModelKey);
		if (list == null) {
			list = new Array();
			this.view.target.data(this.savedModelKey, list);
		}
		return list;
	};
	/**
	 * savedModelList의 이전 model의 id추출
	 */
	this.getPrevModelId = function(id) {
		var list = this.getSavedModelList();
		for (var i = 0 ; i < list.length ; i++) {
			if (list[i].getId() == id && i > 0) {
				return list[i-1].getId();
			}
		}
		return null;
	};
	/**
	 * modelList에 추가 + sort + view에 추가
	 */
	this.addSavedModel = function(model) {
		console.debug("[controller] addSavedModel");
		var list = this.getSavedModelList();
		list.push(model);
		list.sortData("data." + this.sortKey, this.sortIsDescending);
		var prevModelId = this.getPrevModelId(model.getId());
		this.view.add(model, prevModelId);
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
	};
	this.removeSavedModel = function(id) {
		console.debug("[controller] removeSavedModel");
		this.view.remove(id);
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
	_initialize();
	_initializeEvents();
	_initializeExternalEvents();
};
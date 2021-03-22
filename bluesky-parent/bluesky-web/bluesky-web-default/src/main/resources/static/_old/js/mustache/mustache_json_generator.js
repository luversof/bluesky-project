/**
 * Mustache의 Template 에서 대상이 되는 JSON format을 추출하기 위한 라이브러리
 */
$(document).ready(function() {
	var MustacheJSONGenerator = function(config) {
		var _config = config;
		
		var _getPreservedWord = function() {
			if (_config["reservedWord"] !== undefined) {
				return _config["reservedWord"]
			}
			$.ajax({
				url : "/js/contents_reservedWord.js",
				async : false
			});
			_config["reservedWord"] = reservedWord;
		};
		_getPreservedWord();
		
		var _isReservedWord = function(word) {
			var isReserved = false; 
			for (var i = 0; i < _config.reservedWord.length; i++) {
				if (_config.reservedWord[i] === word) {
					isReserved = true;
					break;
				}
			}
			return isReserved;
		};
		
		// reservedWord는 true, false값만 가지는 boolean으로 예상? -> TODO 여러 타입의 예약어에 대한 개발을 해야함
		var _generator = function(tokens, json) {
			_generatorInner(tokens, json);
			_removeRepeatWordJson(json, json, []);
			_repositionReservedWordJson(json, json);
		};
		

		/**
		 * template -> json 을 생성한 이후 예약어가 있는 경우 해당 json의 최상위로 빼내는 처리를 하는 함수
		 * 예약어는 무조건 false라는 값으로 설정을 한다
		 * 예약어는 {{#isLogin}} {{/isLogin}} 과 같이 사용하여 array형태로 넘어온다.
		 */
		var _repositionReservedWordJson = function(json, root) {
			for (var key in json) {
				if (json[key].constructor === Array && _isReservedWord(key)){
					for (var key2 in json[key][0]) {
						json[key2] = json[key][0][key2];
					}
					delete json[key];
					root[key] = false;
				} else if (json[key].constructor === Array) {
					for (var i = 0 ; i < json[key].length; i++) {
						_repositionReservedWordJson(json[key][i], root);
					}
				}
			}
		};
		// 상위 리스트에서 사용하는 키와 동일한 이름이 하위리스트에 있는 경우 하위의 키는 제거함
		var _removeRepeatWordJson = function(json, root, upperKeyList) {
			var isRoot = json === root;
			
			// 체크대상인지 확인 후 체크
			for (var key in json) {
				if (json[key].constructor !== Array) {
					if ($.inArray(key, upperKeyList) > 0) {
						delete json[key];
					}
				}
			}

			//현재 위치의 체크가 끝난 이후 keyList를 현재 위치의 값으로 갱신
			var keyList = [];
			for (var i = 0 ; i < upperKeyList.length ; i++) {
				keyList.push(upperKeyList[i]);
			}
			for (var key in json) {
				if (json[key].constructor !== Array) {
					keyList.push(key);
				}
			}
			
			// 하위 depth array에 대해 체크 요청
			for (var key in json) {
				if (json[key].constructor === Array){
					for (var i = 0 ; i < json[key].length; i++) {
						_removeRepeatWordJson(json[key][i], root, keyList);
					}
				}
			}
		};
		
		/**
		 * data에 대해 초기화 처리를 하는 함수
		 * 예약어가 아닌 모든 값을 초기화 한다.
		 * 내부 배열은 첫번째 배열만 세팅한다. 
		 */
		var _initDataValue = function(data) {
			for (var key in data) {
				if (data[key].constructor === Array) {
					for (var i = 0 ; i < 1; i++) {
						_initDataValue(data[key][i]);
					}
				} else {
					if (!_isReservedWord(key)) {
						data[key] = key;
					}
				}
			}
		};
		
		
		var _isBbsListAionUserInfoListEditor = function(name) {
			return (new RegExp("bbsListAionUserInfo-.*").test(name)); 
		};

		
		// 기본 token은 length : 4 인 array의 집합
		// 리스트 token인 경우 length : 6, index 4에 하위 tokne이 있음
		// 규칙 1. token[0] === "name" 또는 "&" 인 경우 token[1]이 객체의 이름
		// 규칙 2. token[0] === "text"인 경우는 패스
		// 규칙 3. token[0] === "#" or "^" 인 경우는 boolean값, 그 안에 array가 있을거임
		
		// 규칙 4. token[0] === "#" or "^" 인 경우 예약어리스트 인 경우 기본 생성 변수를 넣어줘야함  - {{#bbsListUserInfo-test}} 같은 경우 게시물 링크 입력 폼을 추가함 
		var _generatorInner = function(tokens, json) {
			for (var i = 0 ; i < tokens.length ; i++) {
				if (tokens[i].constructor === Array) {
					_generatorInner(tokens[i], json);
				}
				// text는 일반 html template이므로 넘어감
				if (tokens[0] === "text") {
					break;
				}
				// tokens[1]의 값이 #또는 ^인 경우 arrayList를 만들자.\
				// #isLogin, ^isLogin처럼 같이 쓰는 경우가 있으므로 선행 결과가 있는지 체크하여 arrayList를 생성 
				if (tokens[0] === "#" || tokens[0] === "^") {
					if (json[tokens[1]] === undefined) {
						json[tokens[1]] = [];
						json[tokens[1]][0] = {};
						
						if (_isBbsListAionUserInfoListEditor(tokens[1])) {
							json[tokens[1]][0]["bbsListAionUserInfoLink-링크"] = "";
							json[tokens[1]][0]["캐릭터이름"] = "";
							json[tokens[1]][0]["서버ID"] = "";
							json[tokens[1]][0]["캐릭터ID"] = "";
							json[tokens[1]][0]["종족ID"] = "";
							json[tokens[1]][0]["캐릭터홈"] = "";
						}
					}
					_generatorInner(tokens[4], json[tokens[1]][0]);
					break;
				}
				
				// 기본 template 용법 사용의 경우 ( {{키}} 는 name, {{{키}}}는 & 가 0번 index에 위치함
				if (tokens[0] === "name" || tokens[0] === "&") {
					json[tokens[1]] = tokens[1];
					break;
				}
			}
			return json;
		};
		
		var _merge = function(template, data) {
			var json = {};
			_generator(Mustache.parse(template), json);
			return _mergeInner(json, data);
		};
		
		var _mergeFromList = function(templateList, data) {
			var json = {};
			for (var i = 0 ; i < templateList.length; i++) {
				_generator(Mustache.parse(templateList[i]), json);
			}
			return _mergeInner(json, data);
		};
		
		/**
		 * data를 기준으로 jsonFormat을 다시 생성하는 함수
		 * 기본이 되는 jsonFormat은 불변으로 건드리지 않으며 결과 json을 계속 생성한다.
		 * jsonFormat + data => jsonResult;
		 */
		var _mergeInner = function(jsonFormat, data, jsonResult) {
			if (!jsonResult) {
				jsonResult = $.extend(true, {}, jsonFormat);
			}
			for (var key in jsonResult) {
				// 배열 형태인 경우 리스트에 대해 머지 처리를 해야함
				// data쪽이 length가 더 많으면 해당 수 만큼 생성처리해야함
				// 동일한 inner array 있더라도 각 array의 length는 다를 수 있음.
				if (jsonResult[key].constructor === Array && data[key] && data[key].constructor === Array) {
					var length = data[key].length; // 이건 무조건 1임 (format은 list의 data 표본이 1개이므로)
					if (length > 0) {
						for (var i = 0; i < length ; i++) {
							if (!jsonResult[key][i]) {
								jsonResult[key][i] = $.extend(true, {}, jsonFormat[key][0]);
							}
							_mergeInner(jsonFormat[key][0], data[key][i], jsonResult[key][i]);
						}
					}
				} else if (data[key]) {
					jsonResult[key] = data[key];
				} else {
					jsonResult[key] = jsonFormat[key];
				}
			}
			return jsonResult;
		};

		
		return {
			/**
			 * template 을 기준으로 json 기본 형태를 생성
			 */
			generator : function(template) {
				var json = {};
				_generator(Mustache.parse(template), json);
				return json;
			},
			/**
			 * template을 기준으로 json 기본형태를 생성 후
			 * 기존에 존재하던 data와 비교하여 기존 data의 키가 일치한 값들에 대해 merge처리
			 */
			merge : function(template, data) {
				return _merge(template, data);
			},
			/**
			 * 저장을 위해 예약어를 제거한 json을 추출하기 위한 함수
			 * 선행처리 (_repositionReservedWordJson) 로 예약어는 모두 최상위로 빠져있으므로 최상위에서 예약어가 있는지만 확인하여 제거하면 됨.
			 */		
			getJsonForSave : function(data) {
				for (var key in data) {
					if (_isReservedWord(key)) {
						delete data[key];
					}
				}
			},
			/**
			 * 편집을 위해 예약어가 추가된 형태의 json, 혹은 해당 json에 예약어가 있으면 해당 예약어 리스트를 반환하는 함수가 필요함
			 * -> 이거 언제 쓸라고 만들려다 말았더라?
			 */
			getJsonForEdit : function() {
				
			},
			/**
			 * 여러 template 을 기준으로 json 기본 형태를 생성하여 합친다.
			 */
			generatorFromList : function(templateList) {
				var json = {};
				for (var i = 0 ; i < templateList.length ; i++) {
					_generator(Mustache.parse(templateList[i]), json);
				}
				return json;
			},
			/**
			 * 여러 template 을 기준으로 json 기본 형태를 생성 후
			 * 기존에 존재하던 data와 비교하여 기존 data의 키가 일치한 값들에 대해 merge 처리
			 */
			mergeFromList : function(templateList, data) {
				var result = _mergeFromList(templateList, data);
				return result ;
			}, 
			
			initDataValue : function(data) {
				_initDataValue(data);
			}
		}
	};
	
	var config = {
	};
	
	$.mustacheJSONGenerator = MustacheJSONGenerator(config);
});
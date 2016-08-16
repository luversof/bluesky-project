/**
 * Mustache의 Template 에서 사용하는 JSON data를 기준으로 editor form input box를 생성하는 라이브러리
 * mustache_json_generator를 기반으로 기능을 확장
 */
$(document).ready(function() {

	var MustacheJSONEditor = function(config) {
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
		}
		_getPreservedWord();
		
		var contentsJsonEditor = $.contentsMenu.getTemplate("contentsJsonEditor", "/mustache/contents/contentsJsonEditor.html");
		var contentsJsonEditorListGroup = $.contentsMenu.getTemplate("contentsJsonEditorListGroup", "/mustache/contents/contentsJsonEditorListGroup.html");
		var contentsJsonEditorListGroupPart = $.contentsMenu.getTemplate("contentsJsonEditorListGroupPart", "/mustache/contents/contentsJsonEditorListGroupPart.html");
		
		var _form = null;
		
		var _initForm = function() {
			_form = $("<form />").addClass("contentsJsonEditor form-horizontal");
		}
		
		var _isFileEditor = function(word) {
			return (new RegExp("file-.*").test(word));
		}
		
		var _isBbsListAionUserInfoLinkEditor = function(word) {
			return (new RegExp("bbsListAionUserInfoLink-.*").test(word)); 
		}
		
		var _isBbsListAionUserInfoListEditor = function(name) {
			return (new RegExp("bbsListAionUserInfo-.*").test(name)); 
		}
		
		/**
		 * select 기능 예약어 여부 확인
		 * {{select-이름-옵션_옵션_옵션}} 과 같은 형태
		 */
		var _isSelectEditor = function(word) {
			return (new RegExp("select-.*").test(word));
		}
		var _getSelectList = function(word) {
			var parts = word.split("-");
			return parts[2].split("_");
		}
		
		var _isReservedWord = function(word) {
			var isReserved = false;
			if (_isFileEditor(word) || _isSelectEditor(word) || _isBbsListAionUserInfoLinkEditor(word)) {
				isReserved = true; 
			}
			for (var i = 0; i < _config.reservedWord.length; i++) {
				if (_config.reservedWord[i] === word) {
					isReserved = true;
					break;
				}
			}
			return isReserved;
		}
		
		/**
		 * 예약어를 사용하는 경우 해당 예약어에 대해 선택 기능을 제공한다.
		 * 예약어는 data의 최상위에서만 확인한다. (json 생성 후 저장 시 예약어 키는 최상위로 빼서 저장한다는 전제조건이 있음) 
		 * isLogin -> isLoginEditor, isBanned -> isBannedEditor와 같이 영역을 지정한다.
		 */
		var _makeEditorReservedWord = function(data) {
			var renderData = {};
			for (var key in data) {
				for (var i = 0 ; i < _config.reservedWord.length; i++) {
					if (key === _config.reservedWord[i] && data[key] !== undefined) {
						renderData[key + "Editor"] = true;
						renderData.isReservedWord = true;
					}
				}
			}
			if (!renderData.isReservedWord) {
				return;
			}
			_form.append(
				$("<div />").addClass("panel panel-info")
				.append($("<div />").addClass("panel-heading").text("미리보기 선택 옵션"))
				.append($("<div />").addClass("panel-body").append(Mustache.render(contentsJsonEditor, renderData)))
			);
		}
		
		var _makeEditor = function(data, parentKey, index) {
			// json key 순서중 array는 제일 뒤에 처리를 하기 위해 for 문을 2번으로 나누었음
			for (var key in data) {
				// 배열인 경우 리스트에 대한 폼을 만들어야 하겠지? 폼안에 그 대상을 넣어야 하는데..
				if (data[key].constructor !== Array) {
					var name = "";
					if (parentKey !== undefined) {
						name = parentKey;
					}
					if (index !== undefined) {
						name += "[" + index + "]";
					}
					name = (name === "") ? key : name += "[" + key + "]"
					var renderData = {
						key : key,		// template에서 지정한 키워드
						name : name,		// 전체 이름 ex: contacts[0][first-name]
						data : data[key],	// 저장된 data
						getLabel : function() {
							var label = key;
							if (_isFileEditor(key)) {
								label = key.replace("file-", "");
							}
							if (_isSelectEditor(key)) {
								label = key.split("-")[1];
							}
							if (_isBbsListAionUserInfoLinkEditor(key)) {
								label = key.split("-")[1]; 
							}
							return label;
						},
						isFileEditor : function(key) {
							return _isFileEditor(key);
						},
						isBbsListAionUserInfoLinkEditor : function(key) {
							return _isBbsListAionUserInfoLinkEditor(key);
						},
						/* (s) select관련 함수 */
						isSelectEditor : function(key) {
							return _isSelectEditor(key);
						},
						getSelectList : function(key) {
							return _getSelectList(key)
						},
						isSelected : function(key) {
							return this === data[key];
						},
						/* (e) select관련 함수 */
						isReservedWord : function(key) {
							return _isReservedWord(key);
						}
					}
					_form.append(Mustache.render(contentsJsonEditor, renderData));
				}
			}
			for (var key in data) {
				// 배열인 경우 리스트에 대한 폼을 만들어야 하겠지? 폼안에 그 대상을 넣어야 하는데..
				if (data[key].constructor === Array) {
					var length = data[key].length;
					for (var i = 0 ; i < length; i++) {
						var targetParentKey = parentKey === undefined ? key : parentKey + "[" + index + "][" + key + "]";
						_makeEditor(data[key][i], targetParentKey, i);
					}
				}
			}
		}
		
		/**
		 * data를 통해 추출되는 key list를 계산하여 모두 반환한다.
		 */
		var _getEditorKeyList = function(data, keyList, parentKey, index) {
			for (var key in data) {
				// 배열인 경우 리스트에 대한 폼을 만들어야 하겠지? 폼안에 그 대상을 넣어야 하는데..
				if (data[key].constructor !== Array) {
					var id = "";
					if (parentKey !== undefined) {
						id = parentKey;
					}
					if (index !== undefined) {
						id += "[" + index + "]";
					}
					id = (id === "") ? key : id += "[" + key + "]"
					keyList.push(id);
				}
			}
			for (var key in data) {
				// 배열인 경우 리스트에 대한 폼을 만들어야 하겠지? 폼안에 그 대상을 넣어야 하는데..
				if (data[key].constructor === Array) {
					var length = data[key].length;
					for (var i = 0 ; i < length; i++) {
						var targetParentKey = parentKey === undefined ? key : parentKey + "[" + index + "][" + key + "]";
						_getEditorKeyList(data[key][i], keyList, targetParentKey, i);
					}
				}
			}
			return keyList;
		}
		
		/**
		 * 정규표현식으로 배열인 경우 그룹을 지을 대상 keyList를 호출함
		 * 리스트[0][리스트2][0] 와 같은 key에서 [0] 과 같은 배열 키를 기준으로  리스트[0], [리스트2][0] 를 추출하는 정규 표현식
		 */
		var _patternKeyArray = /(.*?\[\d*\])/g;
		var _getKeyArray = function(key, array) {
			var matches = _patternKeyArray.exec(key);
			if (matches === null) {
				return;
			}
			array.push(matches[0]);
			_getKeyArray(key, array);
		}
		
		/**
		 * keyList에서 배열 속성인 key만 가진 keyGroup을 호출
		 * 중복키를 제거함
		 * 이때 중복키 제거시 순서가 흐트러지면 editor display에 문제가 발생하므로 주의해야함
		 */
		var _getMergedArrayKeyGroup= function(keyList) {
			/**
			 * 그룹 대상 key 를 추출
			 */
			var keyArray = [];
			for (var i = 0 ; i < keyList.length ; i++) {
				var groupKeyArray = [];
				_getKeyArray(keyList[i], groupKeyArray)
				var key = "";
				for (var j = 0 ; j < groupKeyArray.length ; j++) {
					key += groupKeyArray[j];
					keyArray.push(key);
				}
			}
			
			/**
			 * 중복키 제거
			 */
			var uniqueKeyArray = keyArray;
			var prevKeyArray = [];
			for (var i = 0 ; i < uniqueKeyArray.length ; i++) {
				var hasPrevKey = false;
				if (prevKeyArray) {
					for (var j = 0 ; j < prevKeyArray.length ; j++) {
						if (prevKeyArray[j] === uniqueKeyArray[i]) {
							hasPrevKey = true;
							break;
						}
					}
				}
				if (hasPrevKey) {
					uniqueKeyArray.splice(i, 1);
					i--;
				} else {
					prevKeyArray.push(uniqueKeyArray[i]);
				}
			}
			return uniqueKeyArray;
		}
		
		
		/**
		 * key에서 group 정보를 추출
		 * 리스트[0] -> 리스트
		 * 리스트[0][리스트2][0] -> 리스트[0][리스트2]
		 */
		var _patternKeyGroup = /(.*)(?=\[\d*\])/;
		var _getKeyGroup = function(key) {
			var matches = _patternKeyGroup.exec(key);
			if (matches === null) {
				return null;
			}
			return matches[0];
		}
		
		
		
		// editor의 keyList에 배열인 경우가 있으면 각 배열 요소에 대해 grouping 처리를 한다. 
		var _addEditorListOption = function(data, groupList, parentKey) {
			var keyList = _getEditorKeyList(data, []);
			var mergedArrayKeyGroup = _getMergedArrayKeyGroup(keyList);
			var targetEditorListGroupArea;
			var targetEditorListGroupPartArea;
			var keyGroup;		// 각 키 그룹
			for (var i = 0 ; i < mergedArrayKeyGroup.length ; i++) {
				_form.find("[name^='{0}']".format(mergedArrayKeyGroup[i])).each(function(index) {
					var keyGroup = _getKeyGroup(mergedArrayKeyGroup[i]);
					// 그룹 검색 후 최초 위치 앞에 전체를 wrapping 하는 area를 만들고 해당 area에 집어넣는다.
					targetEditorListGroupArea = _form.find("[data-editor-keyGroup='{0}']".format(keyGroup));
					if (targetEditorListGroupArea.size() === 0) {
						var renderData = {
							keyGroup : keyGroup,
							getLabel : function() {
								var label = keyGroup;
								if (_isBbsListAionUserInfoListEditor(keyGroup)) {
									label = keyGroup.split("-")[1];
								}
								return label;
							}
						}
						targetEditorListGroupArea = $(Mustache.render(contentsJsonEditorListGroup, renderData));
						$(this).closest(".form-group").before(targetEditorListGroupArea);
					}
					
					if (index === 0) {
						var renderData = {
							keyGroupPart : mergedArrayKeyGroup[i],
							isFirstIndex : function() {
								return _getIndexFromKeyGroup(mergedArrayKeyGroup[i]) === 0;
							},
							isEnableRemove : function() {
								var index = _getIndexFromKeyGroup(mergedArrayKeyGroup[i]);
								var dataKey = _getDataKeyFromKeyGroup(mergedArrayKeyGroup[i].replace(_patternKeyGroupIndex, ""));
								var length = eval("data" + dataKey).length;
								return length > 1;
							},
							isLastIndex : function() {
								var index = _getIndexFromKeyGroup(mergedArrayKeyGroup[i]);
								var dataKey = _getDataKeyFromKeyGroup(mergedArrayKeyGroup[i].replace(_patternKeyGroupIndex, ""));
								var lastIndex = eval("data" + dataKey).length - 1;
								return index === lastIndex;
							},
							getLabel : function() {
								var label = mergedArrayKeyGroup[i];
								if (_isBbsListAionUserInfoListEditor(mergedArrayKeyGroup[i])) {
									label = mergedArrayKeyGroup[i].split("-")[1];
								}
								return label;
							}
						}
						targetEditorListGroupPartArea = $(Mustache.render(contentsJsonEditorListGroupPart, renderData));
						targetEditorListGroupArea.find(">.panel-body").append(targetEditorListGroupPartArea)
					}
					$(this).closest(".form-group").appendTo(targetEditorListGroupPartArea.find(">.panel-body"));
				});
				// 그룹핑 해야함
			}
			
			
		}
		
		var _getJson = function() {
			return $(".contentsJsonEditor").serializeJSON({ useIntKeysAsArrayIndex : true });
		}
		
		/**
		 * keyGroup을 기준으로 data의 해당 값을 반환
		 * 리스트[0][리스트2] -> data["리스트"][0]["리스트2"] 반환 
		 */
		var _getDataFromKeyGroup = function(keyGroup) {
			//[를 기준으로 잘라내고 ]를 replace로 제거 후 data에서 반환한다.
			var data = _getJson();
			var keyParts = keyGroup.split("[");
			var dataPart = data;
			for (var i = 0 ; i < keyParts.length ; i++) {
				dataPart = dataPart[keyParts[i].replace("]", "")];
			}
			return dataPart;
		}
		
		/**
		 * keyGroup을 기준으로 data의 해당 키를 반환, eval로 해당 위치를 획득할 수 있게 한다.
		 * 리스트[0][리스트2] -> ["리스트"][0]["리스트2"]
		 */
		var _getDataKeyFromKeyGroup = function(keyGroup) {
			var keyParts = keyGroup.split("[");
			var dataKey = "";
			
			for (var i = 0 ; i < keyParts.length ; i++) {
				var targetKey = keyParts[i].replace("]", "");
				if (/^\d*$/.test(targetKey)) {
					dataKey += "[{0}]".format(targetKey);
				} else {
					dataKey += "['{0}']".format(targetKey);
				}
			}
			return dataKey;
		}
		
		var _patternKeyGroupIndex = /\[\d*\]$/;
		var _getIndexFromKeyGroup = function(keyGruop) {
			var matches = _patternKeyGroupIndex.exec(keyGruop)
			if (matches === null) {
				return null;
			}
			return Number(matches[0].replace("[", "").replace("]", ""));
		}
		
		return {
			/**
			 * json 으로 editor form을 생성
			 */
			makeEditor : function(data) {
				_initForm();
				_makeEditorReservedWord(data);
				_makeEditor(data);
				_addEditorListOption(data);
				return _form;
			},
		
			getJson : function() {
				return _getJson();
			},
			getEditorKeyList : function(data) {
				return _getEditorKeyList(data, []);
			},
			
			
			/**
			 * editor의 리스트를 추가하는 이벤트 발생 시 수행
			 * editor를 구현하지 않고 기본이 되는 json data를 수정하여 해당 json을 반환함.
			 */
			listGroupAdd : function(target) {
				var keyGroup = target.closest(_config.area.listGroup).attr("data-editor-keyGroup");
				var dataPart = _getDataFromKeyGroup(keyGroup);
				var dataKey = _getDataKeyFromKeyGroup(keyGroup);
				var data = _getJson();
				var targetData = $.extend(true, {}, dataPart[0]);
				$.mustacheJSONGenerator.initDataValue(targetData);
				eval("data" + dataKey).push(targetData);
				return data;
			}, 
			listGroupPartRemove : function(target) {
				var keyGroupPart = target.closest(_config.area.listGroupPart).attr("data-editor-keyGroup-part");
				var index = _getIndexFromKeyGroup(keyGroupPart);
				var dataKey = _getDataKeyFromKeyGroup(keyGroupPart.replace(_patternKeyGroupIndex, ""));
				var data = _getJson();
				eval("data" + dataKey).splice(index, 1);
				return data;
			},
			listGroupPartUp : function(target) {
				var keyGroupPart = target.closest(_config.area.listGroupPart).attr("data-editor-keyGroup-part");
				var index = _getIndexFromKeyGroup(keyGroupPart);
				var dataKey = _getDataKeyFromKeyGroup(keyGroupPart.replace(_patternKeyGroupIndex, ""));
				var data = _getJson();
				var target = eval("data" + dataKey);
				target.splice(index - 1, 2, target[index], target[index - 1]);
				return data;
			},
			listGroupPartDown : function(target) {
				var keyGroupPart = target.closest(_config.area.listGroupPart).attr("data-editor-keyGroup-part");
				var index = _getIndexFromKeyGroup(keyGroupPart);
				var dataKey = _getDataKeyFromKeyGroup(keyGroupPart.replace(_patternKeyGroupIndex, ""));
				var data = _getJson();
				var target = eval("data" + dataKey);
				target.splice(index, 2, target[index + 1], target[index]);
				return data;
			},
			/**
			 * 게시글 추출로 캐릭터 정보를 추가하는 경우
			 */
			getBbsListAionUserInfo : function(targetInput) {
				var targetUrl = targetInput.val();
				if (targetUrl === "") {
					alert("추출할 게시물의 url을 입력해 주세요.");
					return;
				}
				
				var bbsList = $.ajax({
					url : "/snow2/search/getAionBbsListByTargetUrl",
					data : { targetUrl : targetUrl },
					async : false
				}).responseJSON;

				if (!bbsList) {
					alert("해당 정보를 호출할 수 없습니다.");
					return;
				}
				var keyGroupPart = targetInput.closest("[data-editor-keygroup-part]").attr("data-editor-keygroup-part");
				
				var userInfoPart = bbsList.reserved1.split("-");	// serverId, characterId, characterRace, 
				
				
				console.log("bbsList : ", bbsList, bbsList.reserved1, keyGroupPart);
				console.log("대상 : ", "[id='{0}[캐릭터이름]']".format(keyGroupPart), $("[id='{0}[캐릭터이름]']".format(keyGroupPart)).size());
				$("[id='{0}[캐릭터이름]']".format(keyGroupPart)).val(bbsList.writer);
				$("[id='{0}[서버ID]']".format(keyGroupPart)).val(userInfoPart[0]);
				$("[id='{0}[캐릭터ID]']".format(keyGroupPart)).val(userInfoPart[1]);
				$("[id='{0}[종족ID]']".format(keyGroupPart)).val(userInfoPart[2]);
				$("[id='{0}[캐릭터홈]']".format(keyGroupPart)).val("http://aion.plaync.com/characters/{0}/{1}/go".format(userInfoPart[0], Base64.encode(userInfoPart[1])));
				targetInput.trigger("change");
			},
			/**
			 * 메인등극 알림을 발송
			 */
			sendSnow2MainRegisterNotification : function(targetInput) {
				var targetUrl = targetInput.val();
				if (targetUrl === "") {
					alert("추출할 게시물의 url을 입력해 주세요.");
					return;
				}
				
				var sendResponse = $.ajax({
					url : "/snow2/sendSnow2MainRegisterNotification",
					data : { targetUrl : targetUrl },
					async : false
				}).responseJSON;
				
				console.log("sendResponse : ", sendResponse);
				
				if (!sendResponse) {
					alert("해당 정보를 호출할 수 없습니다.");
					return;
				}
			}
		}
	}
	
	var config = {
		area : {
			listGroup : ".editor-list-group",
			listGroupPart : ".editor-list-group-part"
		},
		event : {
			listGroupAdd : ".editor-list-group-add",
			listGroupRemove : ".editor-list-group-remove",
			listGroupUp : ".editor-list-group-up",
			listGroupDown : ".editor-list-group-down"
		}
	}
	
	$.mustacheJSONEditor = MustacheJSONEditor(config);
	
//	$(document).on("click", config.event.listGroupAdd, function(events) {
//		$.mustacheJSONEditor.listGroupAdd($(this));
//	});
	
	
	$(document).on("click", ".getBbsListAionUserInfo", function() {
		var targetInput = $(this).closest(".form-group").find("input");
		$.mustacheJSONEditor.getBbsListAionUserInfo(targetInput);
	});
	
	$(document).on("click", ".sendSnow2MainRegisterNotification", function() {
		var targetInput = $(this).closest(".form-group").find("input");
		$.mustacheJSONEditor.sendSnow2MainRegisterNotification(targetInput);
	});
});
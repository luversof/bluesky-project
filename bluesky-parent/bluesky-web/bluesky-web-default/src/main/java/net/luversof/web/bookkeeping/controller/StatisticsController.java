/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Vladislav Zablotsky
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE. 
 */
package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.bookkeeping.domain.Statistics;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.service.StatisticsService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;

/**
 * 통계의 경우
 * StatisticsSearchInfo에 따른 데이터 호출 부분을 표현함.
 * 전체 자산 현황은 자산 정보 설정 부분에서 확인 가능
 * @author bluesky
 *
 */
//@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsController {
	
	@Autowired
	private StatisticsService statisticsService;
	
	@PostAuthorize("(returnObject == null or returnObject.size() == 0) or returnObject.get(0).entryGroup.bookkeeping.userId == authentication.principal.id")
	@GetMapping
	public List<Statistics> getEntryList(@Validated(StatisticsSearchInfo.SelectEntryList.class) StatisticsSearchInfo statisticsSearchInfo, Authentication authentication) {
		return statisticsService.selectStatistics(statisticsSearchInfo);
	}
}

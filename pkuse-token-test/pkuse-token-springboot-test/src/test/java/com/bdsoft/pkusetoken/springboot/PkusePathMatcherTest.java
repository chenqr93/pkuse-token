/*
 * Copyright 2020-2099 sa-token.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bdsoft.pkusetoken.springboot;

import com.bdsoft.pkusetoken.spring.pathmatch.PkusePathMatcherHolder;
import com.bdsoft.pkusetoken.spring.pathmatch.PkusePathPatternParserUtil;
import com.bdsoft.pkusetoken.spring.pathmatch.PkusePatternsRequestConditionHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

/**
 * PkusePathMatcher 路由匹配测试
 * 
 * @author click33  
 *
 */
public class PkusePathMatcherTest {

	// 开始 
	@BeforeAll
    public static void beforeClass() {
    	
    }

	// 结束 
    @AfterAll
    public static void afterClass() {
    	
    }

    // 测试，PkusePathMatcherHolder
    @Test
    public void testSaPathMatcherHolder() {
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/user/get", "/user/get"));
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/user/*", "/user/get"));
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/user/**", "/user/get/list"));
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/user/**/page", "/user/get/list/page"));
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/user/get/{id}", "/user/get/123"));
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/user/get/{id}/page", "/user/get/123/page"));
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/*.js", "/sa.js"));
        Assertions.assertTrue(PkusePathMatcherHolder.getPathMatcher().match("/user/**/*.js", "/user/sa.js"));

        // PkusePathMatcherHolder 无法匹配斜杠后缀
        Assertions.assertFalse(PkusePathMatcherHolder.getPathMatcher().match("/user/get", "/user/get/"));
    }

    // 测试，PkusePatternsRequestConditionHolder
    @Test
    public void testSaPatternsRequestConditionHolder() {

        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/get", "/user/get"));
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/*", "/user/get"));
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/**", "/user/get/list"));
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/**/page", "/user/get/list/page"));
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/get/{id}", "/user/get/123"));
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/get/{id}/page", "/user/get/123/page"));
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/*.js", "/sa.js"));
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/**/*.js", "/user/sa.js"));

        // PkusePatternsRequestConditionHolder 可匹配斜杠后缀
        Assertions.assertTrue(PkusePatternsRequestConditionHolder.match("/user/get", "/user/get/"));
    }

    // 测试，testSaPathPatternParserUtil
    @Test
    public void testSaPathPatternParserUtil() {

        Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/get", "/user/get"));
        Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/*", "/user/get"));
        Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/**", "/user/get/list"));
        // PathPatternParser 不允许 ** 后面还有内容
        // Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/**/page", "/user/get/list/page"));
        Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/get/{id}", "/user/get/123"));
        Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/get/{id}/page", "/user/get/123/page"));
        Assertions.assertTrue(PkusePathPatternParserUtil.match("/*.js", "/sa.js"));
        // Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/**/*.js", "/user/sa.js"));

        // PkusePathPatternParserUtil
        //      在 springboot2.x 版本下 可匹配斜杠后缀
        //      在 springboot3.x 版本下 不可匹配斜杠后缀
        if(SpringBootVersion.getVersion().startsWith("2.")) {
            Assertions.assertTrue(PkusePathPatternParserUtil.match("/user/get", "/user/get/"));
        }
        if(SpringBootVersion.getVersion().startsWith("3.")) {
            Assertions.assertFalse(PkusePathPatternParserUtil.match("/user/get", "/user/get/"));
        }
    }












}

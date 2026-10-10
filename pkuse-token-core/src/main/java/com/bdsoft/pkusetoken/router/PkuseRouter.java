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
package com.bdsoft.pkusetoken.router;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.exception.BackResultException;
import com.bdsoft.pkusetoken.exception.StopMatchException;
import com.bdsoft.pkusetoken.fun.PkuseFunction;
import com.bdsoft.pkusetoken.fun.PkuseParamFunction;
import com.bdsoft.pkusetoken.fun.PkuseParamRetFunction;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;

import java.util.List;

/**
 * 路由匹配操作工具类
 *
 * <p> 提供了一系列的路由匹配操作方法，一般用在全局拦截器、过滤器做路由拦截鉴权。 </p>
 * <p> 简单示例： </p>
 * <pre>
 *    	// 指定一条 match 规则
 *    	PkuseRouter
 *    	   	.match("/**")    // 拦截的 path 列表，可以写多个
 *   	   	.notMatch("/user/doLogin")        // 排除掉的 path 列表，可以写多个
 *   	   	.check(r->StpUtil.checkLogin());        // 要执行的校验动作，可以写完整的 lambda 表达式
 * </pre>
 *
 * @author click33
 * @since 1.27.0
 */
public class PkuseRouter {

	private PkuseRouter() {
	}
	
	// -------------------- 路由匹配相关 -------------------- 
	
	/**
	 * 路由匹配
	 * @param pattern 路由匹配符 
	 * @param path 被匹配的路由  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(String pattern, String path) {
		return PkuseStrategy.instance.routeMatcher.apply(pattern, path);
	}

	/**
	 * 路由匹配   
	 * @param patterns 路由匹配符集合 
	 * @param path 被匹配的路由  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(List<String> patterns, String path) {
		if(patterns == null) {
			return false;
		}
		for (String pattern : patterns) {
			if(isMatch(pattern, path)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 路由匹配   
	 * @param patterns 路由匹配符数组  
	 * @param path 被匹配的路由  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(String[] patterns, String path) {
		if(patterns == null) {
			return false;
		}
		for (String pattern : patterns) {
			if(isMatch(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Http请求方法匹配 
	 * @param methods Http请求方法断言数组  
	 * @param methodString Http请求方法
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(PkuseHttpMethod[] methods, String methodString) {
		if(methods == null) {
			return false;
		}
		for (PkuseHttpMethod method : methods) {
			if(method == PkuseHttpMethod.ALL || (method != null && method.toString().equalsIgnoreCase(methodString))) {
				return true;
			}
		}
		return false;
	}
	
	// ------ 使用当前URI匹配 
	
	/**
	 * 路由匹配 (使用当前URI) 
	 * @param pattern 路由匹配符 
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrURI(String pattern) {
		return isMatch(pattern, PkuseHolder.getRequest().getRequestPath());
	}

	/**
	 * 路由匹配 (使用当前URI) 
	 * @param patterns 路由匹配符集合 
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrURI(List<String> patterns) {
		return isMatch(patterns, PkuseHolder.getRequest().getRequestPath());
	}

	/**
	 * 路由匹配 (使用当前URI) 
	 * @param patterns 路由匹配符数组 
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrURI(String[] patterns) {
		return isMatch(patterns, PkuseHolder.getRequest().getRequestPath());
	}

	/**
	 * Http请求方法匹配 (使用当前请求方式) 
	 * @param methods Http请求方法断言数组  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrMethod(PkuseHttpMethod[] methods) {
		return isMatch(methods, PkuseHolder.getRequest().getMethod());
	}
	

	// -------------------- 开始匹配 -------------------- 
	
	/**
	 * 初始化一个PkuseRouterStaff，开始匹配
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff newMatch() {
		return new PkuseRouterStaff();
	}

	// ----------------- path匹配 
	
	/**
	 * 路由匹配 
	 * @param patterns 路由匹配符集合
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff match(String... patterns) {
		return new PkuseRouterStaff().match(patterns);
	}

	/**
	 * 路由匹配排除 
	 * @param patterns 路由匹配符排除数组  
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff notMatch(String... patterns) {
		return new PkuseRouterStaff().notMatch(patterns);
	}

	/**
	 * 路由匹配 
	 * @param patterns 路由匹配符集合 
	 * @return 对象自身 
	 */
	public static PkuseRouterStaff match(List<String> patterns) {
		return new PkuseRouterStaff().match(patterns);
	}

	/**
	 * 路由匹配排除 
	 * @param patterns 路由匹配符排除集合 
	 * @return 对象自身 
	 */
	public static PkuseRouterStaff notMatch(List<String> patterns) {
		return new PkuseRouterStaff().notMatch(patterns);
	}

	// ----------------- Method匹配 
	
	/**
	 * Http请求方式匹配 (Enum) 
	 * @param methods Http请求方法断言数组  
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff match(PkuseHttpMethod... methods) {
		return new PkuseRouterStaff().match(methods);
	}

	/**
	 * Http请求方法匹配排除 (Enum) 
	 * @param methods Http请求方法断言排除数组  
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff notMatch(PkuseHttpMethod... methods) {
		return new PkuseRouterStaff().notMatch(methods);
	}

	/**
	 * Http请求方法匹配 (String)  
	 * @param methods Http请求方法断言数组  
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff matchMethod(String... methods) {
		return new PkuseRouterStaff().matchMethod(methods);
	}

	/**
	 * Http请求方法匹配排除 (String) 
	 * @param methods Http请求方法断言排除数组  
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff notMatchMethod(String... methods) {
		return new PkuseRouterStaff().notMatchMethod(methods);
	}
	
	// ----------------- 条件匹配 

	/**
	 * 根据 boolean 值进行匹配 
	 * @param flag boolean值 
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff match(boolean flag) {
		return new PkuseRouterStaff().match(flag);
	}

	/**
	 * 根据 boolean 值进行匹配排除 
	 * @param flag boolean值 
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff notMatch(boolean flag) {
		return new PkuseRouterStaff().notMatch(flag);
	}
	
	/**
	 * 根据自定义方法进行匹配 (lazy) 
	 * @param fun 自定义方法
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff match(PkuseParamRetFunction<Object, Boolean> fun) {
		return new PkuseRouterStaff().match(fun);
	}

	/**
	 * 根据自定义方法进行匹配排除 (lazy) 
	 * @param fun 自定义排除方法
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff notMatch(PkuseParamRetFunction<Object, Boolean> fun) {
		return new PkuseRouterStaff().notMatch(fun);
	}

	
	// -------------------- 直接指定check函数 -------------------- 
	
	/**
	 * 路由匹配，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符
	 * @param fun 要执行的校验方法 
	 * @return /
	 */
	public static PkuseRouterStaff match(String pattern, PkuseFunction fun) {
		return new PkuseRouterStaff().match(pattern, fun);
	}

	/**
	 * 路由匹配，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符
	 * @param fun 要执行的校验方法 
	 * @return /
	 */
	public static PkuseRouterStaff match(String pattern, PkuseParamFunction<PkuseRouterStaff> fun) {
		return new PkuseRouterStaff().match(pattern, fun);
	}

	/**
	 * 路由匹配 (并指定排除匹配符)，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符 
	 * @param excludePattern 要排除的路由匹配符 
	 * @param fun 要执行的方法 
	 * @return /
	 */
	public static PkuseRouterStaff match(String pattern, String excludePattern, PkuseFunction fun) {
		return new PkuseRouterStaff().match(pattern, excludePattern, fun);
	}

	/**
	 * 路由匹配 (并指定排除匹配符)，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符 
	 * @param excludePattern 要排除的路由匹配符 
	 * @param fun 要执行的方法 
	 * @return /
	 */
	public static PkuseRouterStaff match(String pattern, String excludePattern, PkuseParamFunction<PkuseRouterStaff> fun) {
		return new PkuseRouterStaff().match(pattern, excludePattern, fun);
	}

	
	// -------------------- 提前退出 -------------------- 
	
	/**
	 * 停止匹配，跳出函数 (在多个匹配链中一次性跳出Auth函数) 
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff stop() {
		throw new StopMatchException();
	}

	/**
	 * 停止匹配，结束执行，向前端返回结果 
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff back() {
		throw new BackResultException("");
	}
	
	/**
	 * 停止匹配，结束执行，向前端返回结果 
	 * @param result 要输出的结果 
	 * @return PkuseRouterStaff
	 */
	public static PkuseRouterStaff back(Object result) {
		throw new BackResultException(result);
	}

}

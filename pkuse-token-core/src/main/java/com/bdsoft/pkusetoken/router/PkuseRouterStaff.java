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

import java.util.List;

import com.bdsoft.pkusetoken.exception.BackResultException;
import com.bdsoft.pkusetoken.exception.StopMatchException;
import com.bdsoft.pkusetoken.fun.PkuseFunction;
import com.bdsoft.pkusetoken.fun.PkuseParamFunction;
import com.bdsoft.pkusetoken.fun.PkuseParamRetFunction;

/**
 * 路由匹配操作对象 
 * 
 * @author click33
 * @since 1.27.0
 */
public class PkuseRouterStaff {

	/**
	 * 是否命中的标记变量 
	 */
	public boolean isHit = true;
	
	/**
	 * @return 是否命中 
	 */
	public boolean isHit() {
		return isHit;
	}

	/**
	 * @param isHit 命中标记 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff setHit(boolean isHit) {
		this.isHit = isHit;
		return this;
	}

	/**
	 * 重置命中标记为 true 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff reset() {
		this.isHit = true;
		return this;
	}
	
	
	// ----------------- path匹配 
	
	/**
	 * 路由匹配 
	 * @param patterns 路由匹配符数组  
	 * @return 对象自身 
	 */
	public PkuseRouterStaff match(String... patterns) {
		if(isHit)  {
			isHit = PkuseRouter.isMatchCurrURI(patterns);
		}
		return this;
	}

	/**
	 * 路由匹配排除 
	 * @param patterns 路由匹配符排除数组  
	 * @return 对象自身 
	 */
	public PkuseRouterStaff notMatch(String... patterns) {
		if(isHit)  {
			isHit = !PkuseRouter.isMatchCurrURI(patterns);
		}
		return this;
	}

	/**
	 * 路由匹配 
	 * @param patterns 路由匹配符集合 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff match(List<String> patterns) {
		if(isHit)  {
			isHit = PkuseRouter.isMatchCurrURI(patterns);
		}
		return this;
	}

	/**
	 * 路由匹配排除 
	 * @param patterns 路由匹配符排除集合 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff notMatch(List<String> patterns) {
		if(isHit)  {
			isHit = !PkuseRouter.isMatchCurrURI(patterns);
		}
		return this;
	}

	// ----------------- Method匹配 

	/**
	 * Http请求方法匹配 (Enum) 
	 * @param methods Http请求方法断言数组  
	 * @return 对象自身 
	 */
	public PkuseRouterStaff match(PkuseHttpMethod... methods) {
		if(isHit)  {
			isHit = PkuseRouter.isMatchCurrMethod(methods);
		}
		return this;
	}

	/**
	 * Http请求方法匹配排除 (Enum) 
	 * @param methods Http请求方法断言排除数组  
	 * @return 对象自身 
	 */
	public PkuseRouterStaff notMatch(PkuseHttpMethod... methods) {
		if(isHit)  {
			isHit = !PkuseRouter.isMatchCurrMethod(methods);
		}
		return this;
	}

	/**
	 * Http请求方法匹配 (String) 
	 * @param methods Http请求方法断言数组  
	 * @return 对象自身 
	 */
	public PkuseRouterStaff matchMethod(String... methods) {
		if(isHit)  {
			PkuseHttpMethod [] arr = PkuseHttpMethod.toEnumArray(methods);
			isHit = PkuseRouter.isMatchCurrMethod(arr);
		}
		return this;
	}

	/**
	 * Http请求方法匹配排除 (String) 
	 * @param methods Http请求方法断言排除数组  
	 * @return 对象自身 
	 */
	public PkuseRouterStaff notMatchMethod(String... methods) {
		if(isHit)  {
			PkuseHttpMethod [] arr = PkuseHttpMethod.toEnumArray(methods);
			isHit = !PkuseRouter.isMatchCurrMethod(arr);
		}
		return this;
	}


	// ----------------- 条件匹配 

	/**
	 * 根据 boolean 值进行匹配 
	 * @param flag boolean值 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff match(boolean flag) {
		if(isHit)  {
			isHit = flag;
		}
		return this;
	}

	/**
	 * 根据 boolean 值进行匹配排除 
	 * @param flag boolean值 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff notMatch(boolean flag) {
		if(isHit)  {
			isHit = !flag;
		}
		return this;
	}
	
	/**
	 * 根据自定义方法进行匹配 (lazy)  
	 * @param fun 自定义方法
	 * @return 对象自身 
	 */
	public PkuseRouterStaff match(PkuseParamRetFunction<Object, Boolean> fun) {
		if(isHit)  {
			isHit = fun.run(this);
		}
		return this;
	}

	/**
	 * 根据自定义方法进行匹配排除 (lazy) 
	 * @param fun 自定义排除方法
	 * @return 对象自身 
	 */
	public PkuseRouterStaff notMatch(PkuseParamRetFunction<Object, Boolean> fun) {
		if(isHit)  {
			isHit = !fun.run(this);
		}
		return this;
	}
	
	
	// ----------------- 函数校验执行 

	/**
	 * 执行校验函数 (无参) 
	 * @param fun 要执行的函数 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff check(PkuseFunction fun) {
		if(isHit)  {
			fun.run();
		}
		return this;
	}
	
	/**
	 * 执行校验函数 (带参) 
	 * @param fun 要执行的函数 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff check(PkuseParamFunction<PkuseRouterStaff> fun) {
		if(isHit)  {
			fun.run(this);
		}
		return this;
	}
	
	/**
	 * 自由匹配 （ 在free作用域里执行stop()不会跳出Auth函数，而是仅仅跳出free代码块 ）
	 * @param fun 要执行的函数 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff free(PkuseParamFunction<PkuseRouterStaff> fun) {
		if(isHit)  {
			try {
				fun.run(this);
			} catch (StopMatchException e) {
				// 跳出 free自由匹配代码块 
			}
		}
		return this;
	}

	
	// ----------------- 直接指定check函数 
	
	/**
	 * 路由匹配，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符
	 * @param fun 要执行的校验方法 
	 * @return /
	 */
	public PkuseRouterStaff match(String pattern, PkuseFunction fun) {
		return this.match(pattern).check(fun);
	}

	/**
	 * 路由匹配，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符
	 * @param fun 要执行的校验方法 
	 * @return /
	 */
	public PkuseRouterStaff match(String pattern, PkuseParamFunction<PkuseRouterStaff> fun) {
		return this.match(pattern).check(fun);
	}

	/**
	 * 路由匹配 (并指定排除匹配符)，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符 
	 * @param excludePattern 要排除的路由匹配符 
	 * @param fun 要执行的方法 
	 * @return /
	 */
	public PkuseRouterStaff match(String pattern, String excludePattern, PkuseFunction fun) {
		return this.match(pattern).notMatch(excludePattern).check(fun);
	}

	/**
	 * 路由匹配 (并指定排除匹配符)，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符 
	 * @param excludePattern 要排除的路由匹配符 
	 * @param fun 要执行的方法 
	 * @return /
	 */
	public PkuseRouterStaff match(String pattern, String excludePattern, PkuseParamFunction<PkuseRouterStaff> fun) {
		return this.match(pattern).notMatch(excludePattern).check(fun);
	}
	
	
	// ----------------- 提前退出 

	/**
	 * 停止匹配，跳出函数 (在多个匹配链中一次性跳出Auth函数) 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff stop() {
		if(isHit) {
			throw new StopMatchException();
		}
		return this;
	}

	/**
	 * 停止匹配，结束执行，向前端返回结果 
	 * @return 对象自身 
	 */
	public PkuseRouterStaff back() {
		if(isHit) {
			throw new BackResultException("");
		}
		return this;
	}
	
	/**
	 * 停止匹配，结束执行，向前端返回结果 
	 * @return 对象自身 
	 * @param result 要输出的结果 
	 */
	public PkuseRouterStaff back(Object result) {
		if(isHit) {
			throw new BackResultException(result);
		}
		return this;
	}

	
}

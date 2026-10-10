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
package com.bdsoft.pkusetoken.strategy;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.fun.strategy.PkuseFirewallCheckFailHandleFunction;
import com.bdsoft.pkusetoken.fun.strategy.PkuseFirewallCheckFunction;
import com.bdsoft.pkusetoken.strategy.hooks.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 防火墙策略
 *
 * @author click33
 * @since 1.40.0
 */
public final class PkuseFirewallStrategy {

	/**
	 * 全局单例引用
	 */
	public static final PkuseFirewallStrategy instance = new PkuseFirewallStrategy();

	/**
	 * 防火墙校验钩子函数集合
	 */
	public List<PkuseFirewallCheckHook> checkHooks = new ArrayList<>();

	private PkuseFirewallStrategy() {
		// 初始化默认的防火墙校验钩子函数集合
		checkHooks.add(PkuseFirewallCheckHookForWhitePath.instance);
		checkHooks.add(PkuseFirewallCheckHookForBlackPath.instance);
		checkHooks.add(PkuseFirewallCheckHookForPathDangerCharacter.instance);
		checkHooks.add(PkuseFirewallCheckHookForPathBannedCharacter.instance);
		checkHooks.add(PkuseFirewallCheckHookForDirectoryTraversal.instance);
		checkHooks.add(PkuseFirewallCheckHookForHost.instance);
		checkHooks.add(PkuseFirewallCheckHookForHttpMethod.instance);
		checkHooks.add(PkuseFirewallCheckHookForHeader.instance);
		checkHooks.add(PkuseFirewallCheckHookForParameter.instance);
	}

	/**
	 * 注册一个防火墙校验 hook
	 * @param checkHook /
	 */
	public void registerHook(PkuseFirewallCheckHook checkHook) {
		PkuseManager.getLog().info("防火墙校验 hook 注册成功: " + checkHook.getClass());
		checkHooks.add(checkHook);
	}

	/**
	 * 注册一个防火墙校验 hook 到第一位，
	 * <b>请注意将 hook 注册到第一位将会优先于白名单的判断，如果您依然希望白名单 hook 保持最高优先级，请调用 registerHookToSecond </b>
	 * @param checkHook /
	 */
	public void registerHookToFirst(PkuseFirewallCheckHook checkHook) {
		PkuseManager.getLog().info("防火墙校验 hook 注册成功: " + checkHook.getClass());
		checkHooks.add(0, checkHook);
	}

	/**
	 * 注册一个防火墙校验 hook 到第二位
	 * @param checkHook /
	 */
	public void registerHookToSecond(PkuseFirewallCheckHook checkHook) {
		PkuseManager.getLog().info("防火墙校验 hook 注册成功: " + checkHook.getClass());
		checkHooks.add(1, checkHook);
	}

	/**
	 * 移除指定类型的防火墙校验 hook
	 * @param hookClass /
	 */
	public void removeHook(Class<? extends PkuseFirewallCheckHook> hookClass) {
		for (PkuseFirewallCheckHook hook : checkHooks) {
			if (hook.getClass().equals(hookClass)) {
				checkHooks.remove(hook);
				PkuseManager.getLog().info("防火墙校验 hook 移除成功: " + hookClass);
				return;
			}
		}
	}

	/**
	 * 防火墙校验函数
	 */
	public PkuseFirewallCheckFunction check = (req, res, extArg) -> {
		for (PkuseFirewallCheckHook checkHook : checkHooks) {
			checkHook.execute(req, res, extArg);
		}
	};

	/**
	 * 当请求 path 校验不通过时地处理方案，自定义示例：
	 * <pre>
	 * 		PkuseFirewallStrategy.instance.checkFailHandle = (e, req, res, extArg) -> {
	 * 			// 自定义处理逻辑 ...
	 *      };
	 * </pre>
	 */
	public PkuseFirewallCheckFailHandleFunction checkFailHandle = null;


}

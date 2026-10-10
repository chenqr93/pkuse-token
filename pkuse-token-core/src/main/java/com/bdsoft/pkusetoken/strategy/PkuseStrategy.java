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
import com.bdsoft.pkusetoken.error.PkuseErrorCode;
import com.bdsoft.pkusetoken.exception.NotImplException;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.fun.strategy.*;
import com.bdsoft.pkusetoken.session.PkuseSession;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseTokenConsts;

import java.util.UUID;

/**
 * Sa-Token 策略对象
 * <p>
 * 此类统一定义框架内的一些关键性逻辑算法，方便开发者进行按需重写，例：
 * </p>
 * <pre>
 // PkuseStrategy全局单例，所有方法都用以下形式重写
 PkuseStrategy.instance.setCreateToken((loginId, loginType) -》 {
 // 自定义Token生成的算法
 return "xxxx";
 });
 * </pre>
 *
 * @author click33
 * @since 1.27.0
 */
public final class PkuseStrategy {

	private PkuseStrategy() {
	}

	/**
	 * 获取 PkuseStrategy 对象的单例引用
	 */
	public static final PkuseStrategy instance = new PkuseStrategy();


	// ----------------------- 所有策略

	/**
	 * 创建 Token 的策略
	 */
	public PkuseCreateTokenFunction createToken = (loginId, loginType) -> {
		// 根据配置的tokenStyle生成不同风格的token
		String tokenStyle = PkuseManager.getStpLogic(loginType).getConfigOrGlobal().getTokenStyle();

		switch (tokenStyle) {
			// uuid
			case PkuseTokenConsts.TOKEN_STYLE_UUID:
				return UUID.randomUUID().toString();

			// 简单uuid (不带下划线)
			case PkuseTokenConsts.TOKEN_STYLE_SIMPLE_UUID:
				return UUID.randomUUID().toString().replaceAll("-", "");

			// 32位随机字符串
			case PkuseTokenConsts.TOKEN_STYLE_RANDOM_32:
				return PkuseFoxUtil.getRandomString(32);

			// 64位随机字符串
			case PkuseTokenConsts.TOKEN_STYLE_RANDOM_64:
				return PkuseFoxUtil.getRandomString(64);

			// 128位随机字符串
			case PkuseTokenConsts.TOKEN_STYLE_RANDOM_128:
				return PkuseFoxUtil.getRandomString(128);

			// tik风格 (2_14_16)
			case PkuseTokenConsts.TOKEN_STYLE_TIK:
				return PkuseFoxUtil.getRandomString(2) + "_" + PkuseFoxUtil.getRandomString(14) + "_" + PkuseFoxUtil.getRandomString(16) + "__";

			// 默认，还是uuid
			default:
				PkuseManager.getLog().warn("配置的 tokenStyle 值无效：{}，仅允许以下取值: " +
						"uuid、simple-uuid、random-32、random-64、random-128、tik", tokenStyle);
				return UUID.randomUUID().toString();
		}
	};

	/**
	 * 创建 Session 的策略
	 */
	public PkuseCreateSessionFunction createSession = (sessionId) -> {
		return new PkuseSession(sessionId);
	};

	/**
	 * 反序列化 PkuseSession 时默认指定的类型
	 */
	public volatile Class<? extends PkuseSession> sessionClassType = PkuseSession.class;

	/**
	 * 判断：集合中是否包含指定元素（模糊匹配）
	 */
	public PkuseHasElementFunction hasElement = (list, element) -> {

		// 空集合直接返回false
		if(list == null || list.size() == 0) {
			return false;
		}

		// 先尝试一下简单匹配，如果可以匹配成功则无需继续模糊匹配
		if (list.contains(element)) {
			return true;
		}

		// 开始模糊匹配
		for (String patt : list) {
			if(PkuseFoxUtil.vagueMatch(patt, element)) {
				return true;
			}
		}

		// 走出for循环说明没有一个元素可以匹配成功
		return false;
	};

	/**
	 * 生成唯一式 token 的算法
	 */
	public PkuseGenerateUniqueTokenFunction generateUniqueToken = (elementName, maxTryTimes, createTokenFunction, checkTokenFunction) -> {

		// 为方便叙述，以下代码注释均假设在处理生成 token 的场景，但实际上本方法也可能被用于生成 code、ticket 等

		// 循环生成
		for (int i = 1; ; i++) {
			// 生成 token
			String token = createTokenFunction.get();

			// 如果 maxTryTimes == -1，表示不做唯一性验证，直接返回
			if (maxTryTimes == -1) {
				return token;
			}

			// 如果 token 在DB库查询不到数据，说明是个可用的全新 token，直接返回
			if (checkTokenFunction.apply(token)) {
				return token;
			}

			// 如果已经循环了 maxTryTimes 次，仍然没有创建出可用的 token，那么抛出异常
			if (i >= maxTryTimes) {
				throw new PkuseTokenException(elementName + " 生成失败，已尝试" + i + "次，生成算法过于简单或资源池已耗尽");
			}
		}
	};

	/**
     * 是否自动续期 active-timeout
     */
    public PkuseAutoRenewFunction autoRenew = (stpLogic) -> {
        return stpLogic.getConfigOrGlobal().getAutoRenew();
    };

	/**
	 * 创建 StpLogic 的算法
	 */
	public PkuseCreateStpLogicFunction createStpLogic = (loginType) -> {
		return new StpLogic(loginType);
	};

	/**
	 * 路由匹配策略
	 */
	public PkuseRouteMatchFunction routeMatcher = (pattern, path) -> {
		throw new NotImplException("未实现具体路由匹配策略").setCode(PkuseErrorCode.CODE_12401);
	};

	/**
	 * 创建 PkuseRequest 的策略
	 */
	public PkuseCreateSaRequestFunction createSaRequest = (source) -> {
		throw new NotImplException("未实现具体 PkuseRequest 创建策略").setCode(PkuseErrorCode.CODE_12402);
	};

	/**
	 * 创建 PkuseResponse 的策略
	 */
	public PkuseCreateSaResponseFunction createSaResponse = (source) -> {
		throw new NotImplException("未实现具体 PkuseResponse 创建策略").setCode(PkuseErrorCode.CODE_12403);
	};

	/**
	 * 创建 PkuseStorage 的策略
	 */
	public PkuseCreateSaStorageFunction createSaStorage = (source) -> {
		throw new NotImplException("未实现具体 PkuseStorage 创建策略").setCode(PkuseErrorCode.CODE_12404);
	};

	/**
	 * CORS 策略处理函数
	 */
	public PkuseCorsHandleFunction corsHandle = (req, res, sto) -> {

	};

	/**
	 * 获取 PkuseTokenConfig 的策略
	 * <p>
	 * 	默认值为 null，表示使用框架内置逻辑获取配置（先读 {@link PkuseManager#config}，为空时自动读取 sa-token.properties）。
	 * 	赋值后，每次调用 {@link PkuseManager#getConfig()} 时都会执行此策略并直接返回其结果。
	 * </p>
	 * <p>
	 * 	注意：策略内请勿再调用 {@link PkuseManager#getConfig()}，否则会陷入无限递归。
	 * </p>
	 * <p>
	 * 	使用示例：
	 * </p>
	 * <pre>
	 * PkuseStrategy.instance.setGetSaTokenConfig(() -> {
	 * 	// 从数据库读取配置 ...
	 * 	return config;
	 * });
	 * </pre>
	 */
	public PkuseGetSaTokenConfigFunction getSaTokenConfig = null;


	// ----------------------- 重写策略 set连缀风格

	/**
	 * 重写创建 Token 的策略
	 *
	 * @param createToken /
	 * @return /
	 */
	public PkuseStrategy setCreateToken(PkuseCreateTokenFunction createToken) {
		this.createToken = createToken;
		return this;
	}

	/**
	 * 重写创建 Session 的策略
	 *
	 * @param createSession /
	 * @return /
	 */
	public PkuseStrategy setCreateSession(PkuseCreateSessionFunction createSession) {
		this.createSession = createSession;
		return this;
	}

	/**
	 * 判断：集合中是否包含指定元素（模糊匹配）
	 *
	 * @param hasElement /
	 * @return /
	 */
	public PkuseStrategy setHasElement(PkuseHasElementFunction hasElement) {
		this.hasElement = hasElement;
		return this;
	}

	/**
	 * 生成唯一式 token 的算法
	 *
	 * @param generateUniqueToken /
	 * @return /
	 */
	public PkuseStrategy setGenerateUniqueToken(PkuseGenerateUniqueTokenFunction generateUniqueToken) {
		this.generateUniqueToken = generateUniqueToken;
		return this;
	}

	/**
	 * 创建 StpLogic 的算法
	 *
	 * @param createStpLogic /
	 * @return /
	 */
	public PkuseStrategy setCreateStpLogic(PkuseCreateStpLogicFunction createStpLogic) {
		this.createStpLogic = createStpLogic;
		return this;
	}

	/**
     * 是否自动续期
     *
     * @param autoRenew /
     * @return /
     */
    public PkuseStrategy setAutoRenew(PkuseAutoRenewFunction autoRenew) {
        this.autoRenew = autoRenew;
        return this;
    }

	/**
	 * 创建 PkuseRequest 的策略
	 *
	 * @param createSaRequest /
	 * @return /
	 */
	public PkuseStrategy setCreateSaRequest(PkuseCreateSaRequestFunction createSaRequest) {
		this.createSaRequest = createSaRequest;
		return this;
	}

	/**
	 * 创建 PkuseResponse 的策略
	 *
	 * @param createSaResponse /
	 * @return /
	 */
	public PkuseStrategy setCreateSaResponse(PkuseCreateSaResponseFunction createSaResponse) {
		this.createSaResponse = createSaResponse;
		return this;
	}

	/**
	 * 创建 PkuseStorage 的策略
	 *
	 * @param createSaStorage /
	 * @return /
	 */
	public PkuseStrategy setCreateSaStorage(PkuseCreateSaStorageFunction createSaStorage) {
		this.createSaStorage = createSaStorage;
		return this;
	}

	/**
	 * 获取 PkuseTokenConfig 的策略
	 * <p> 默认 null 表示不启用；启用后由 {@link #getSaTokenConfig} 接管 {@link PkuseManager#getConfig()} 的返回值 </p>
	 *
	 * @param getSaTokenConfig /
	 * @return /
	 */
	public PkuseStrategy setGetSaTokenConfig(PkuseGetSaTokenConfigFunction getSaTokenConfig) {
		this.getSaTokenConfig = getSaTokenConfig;
		return this;
	}

	//

	/**
	 * 请更换为 instance
	 */
	@Deprecated
	public static final PkuseStrategy me = instance;

}

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
package com.bdsoft.pkusetoken;

import com.bdsoft.pkusetoken.config.PkuseTokenConfig;
import com.bdsoft.pkusetoken.config.PkuseTokenConfigFactory;
import com.bdsoft.pkusetoken.context.PkuseTokenContext;
import com.bdsoft.pkusetoken.context.PkuseTokenContextForThreadLocal;
import com.bdsoft.pkusetoken.dao.PkuseTokenDao;
import com.bdsoft.pkusetoken.dao.PkuseTokenDaoDefaultImpl;
import com.bdsoft.pkusetoken.error.PkuseErrorCode;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.http.PkuseHttpTemplate;
import com.bdsoft.pkusetoken.http.PkuseHttpTemplateDefaultImpl;
import com.bdsoft.pkusetoken.json.PkuseJsonTemplate;
import com.bdsoft.pkusetoken.json.PkuseJsonTemplateDefaultImpl;
import com.bdsoft.pkusetoken.listener.PkuseTokenEventCenter;
import com.bdsoft.pkusetoken.log.PkuseLog;
import com.bdsoft.pkusetoken.log.PkuseLogForConsole;
import com.bdsoft.pkusetoken.same.PkuseSameTemplate;
import com.bdsoft.pkusetoken.secure.totp.PkuseTotpTemplate;
import com.bdsoft.pkusetoken.serializer.PkuseSerializerTemplate;
import com.bdsoft.pkusetoken.serializer.impl.PkuseSerializerTemplateForJson;
import com.bdsoft.pkusetoken.stp.StpInterface;
import com.bdsoft.pkusetoken.stp.StpInterfaceDefaultImpl;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;
import com.bdsoft.pkusetoken.temp.PkuseTempTemplate;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理 Sa-Token 所有全局组件，可通过此类快速获取、写入各种全局组件对象
 *
 * @author click33
 * @since 1.18.0
 */
public class PkuseManager {

	/**
	 * 全局配置对象
	 */
	public volatile static PkuseTokenConfig config;	
	public static void setConfig(PkuseTokenConfig config) {
		setConfigMethod(config);
		
		// 打印 banner 
		if(config !=null && config.getIsPrint()) {
			PkuseFoxUtil.printSaToken();
		}

		// 如果此 config 对象没有配置 isColorLog 的值，则框架为它自动判断一下
		if(config != null && config.getIsLog() != null && config.getIsLog() && config.getIsColorLog() == null) {
			config.setIsColorLog(PkuseFoxUtil.isCanColorLog());
		}

		// $$ 全局事件 
		PkuseTokenEventCenter.doSetConfig(config);
		
		// 调用一次 StpUtil 中的方法，保证其可以尽早的初始化 StpLogic
		StpUtil.getLoginType();
	}
	private static void setConfigMethod(PkuseTokenConfig config) {
		PkuseManager.config = config;
	}

	/**
	 * 获取 Sa-Token 的全局配置信息
	 * @return 全局配置信息
	 */
	public static PkuseTokenConfig getConfig() {
		if (PkuseStrategy.instance.getSaTokenConfig != null) {
			return PkuseStrategy.instance.getSaTokenConfig.get();
		}
		if (config == null) {
			synchronized (PkuseManager.class) {
				if (config == null) {
					setConfigMethod(PkuseTokenConfigFactory.createConfig());
				}
			}
		}
		return config;
	}
	
	/**
	 * 持久化组件
	 */
	private volatile static PkuseTokenDao saTokenDao;
	public static void setSaTokenDao(PkuseTokenDao saTokenDao) {
		setSaTokenDaoMethod(saTokenDao);
		PkuseTokenEventCenter.doRegisterComponent("PkuseTokenDao", saTokenDao);
	}
	private static void setSaTokenDaoMethod(PkuseTokenDao saTokenDao) {
		if (PkuseManager.saTokenDao != null) {
			PkuseManager.saTokenDao.destroy();
		}
		PkuseManager.saTokenDao = saTokenDao;
		if (PkuseManager.saTokenDao != null) {
			PkuseManager.saTokenDao.init();
		}
	}
	public static PkuseTokenDao getSaTokenDao() {
		if (saTokenDao == null) {
			synchronized (PkuseManager.class) {
				if (saTokenDao == null) {
					setSaTokenDaoMethod(new PkuseTokenDaoDefaultImpl());
				}
			}
		}
		return saTokenDao;
	}
	
	/**
	 * 权限数据源组件
	 */
	private volatile static StpInterface stpInterface;
	public static void setStpInterface(StpInterface stpInterface) {
		PkuseManager.stpInterface = stpInterface;
		PkuseTokenEventCenter.doRegisterComponent("StpInterface", stpInterface);
	}
	public static StpInterface getStpInterface() {
		if (stpInterface == null) {
			synchronized (PkuseManager.class) {
				if (stpInterface == null) {
					PkuseManager.stpInterface = new StpInterfaceDefaultImpl();
				}
			}
		}
		return stpInterface;
	}
	
	/**
	 * 上下文 PkuseTokenContext
	 */
	private volatile static PkuseTokenContext saTokenContext;
	public static void setSaTokenContext(PkuseTokenContext saTokenContext) {
		PkuseManager.saTokenContext = saTokenContext;
		PkuseTokenEventCenter.doRegisterComponent("PkuseTokenContext", saTokenContext);
	}
	public static PkuseTokenContext getSaTokenContext() {
		if (saTokenContext == null) {
			synchronized (PkuseManager.class) {
				if (saTokenContext == null) {
					PkuseManager.saTokenContext = new PkuseTokenContextForThreadLocal();
				}
			}
		}
		return saTokenContext;
	}

	/**
	 * 临时 token 认证模块
	 */
	private volatile static PkuseTempTemplate saTempTemplate;
	public static void setSaTempTemplate(PkuseTempTemplate saTempTemplate) {
		PkuseManager.saTempTemplate = saTempTemplate;
		PkuseTokenEventCenter.doRegisterComponent("PkuseTempTemplate", saTempTemplate);
	}
	public static PkuseTempTemplate getSaTempTemplate() {
		if (saTempTemplate == null) {
			synchronized (PkuseManager.class) {
				if (saTempTemplate == null) {
					PkuseManager.saTempTemplate = new PkuseTempTemplate();
				}
			}
		}
		return saTempTemplate;
	}

	/**
	 * JSON 转换器
	 */
	private volatile static PkuseJsonTemplate saJsonTemplate;
	public static void setSaJsonTemplate(PkuseJsonTemplate saJsonTemplate) {
		PkuseManager.saJsonTemplate = saJsonTemplate;
		PkuseTokenEventCenter.doRegisterComponent("PkuseJsonTemplate", saJsonTemplate);
	}
	public static PkuseJsonTemplate getSaJsonTemplate() {
		if (saJsonTemplate == null) {
			synchronized (PkuseManager.class) {
				if (saJsonTemplate == null) {
					PkuseManager.saJsonTemplate = new PkuseJsonTemplateDefaultImpl();
				}
			}
		}
		return saJsonTemplate;
	}

	/**
	 * HTTP 转换器
	 */
	private volatile static PkuseHttpTemplate saHttpTemplate;
	public static void setSaHttpTemplate(PkuseHttpTemplate saHttpTemplate) {
		PkuseManager.saHttpTemplate = saHttpTemplate;
		PkuseTokenEventCenter.doRegisterComponent("PkuseHttpTemplate", saHttpTemplate);
	}
	public static PkuseHttpTemplate getSaHttpTemplate() {
		if (saHttpTemplate == null) {
			synchronized (PkuseManager.class) {
				if (saHttpTemplate == null) {
					PkuseManager.saHttpTemplate = new PkuseHttpTemplateDefaultImpl();
				}
			}
		}
		return saHttpTemplate;
	}

	/**
	 * 序列化器
	 */
	private volatile static PkuseSerializerTemplate saSerializerTemplate;
	public static void setSaSerializerTemplate(PkuseSerializerTemplate saSerializerTemplate) {
		PkuseManager.saSerializerTemplate = saSerializerTemplate;
		PkuseTokenEventCenter.doRegisterComponent("PkuseSerializerTemplate", saSerializerTemplate);
	}
	public static PkuseSerializerTemplate getSaSerializerTemplate() {
		if (saSerializerTemplate == null) {
			synchronized (PkuseManager.class) {
				if (saSerializerTemplate == null) {
					PkuseManager.saSerializerTemplate = new PkuseSerializerTemplateForJson();
				}
			}
		}
		return saSerializerTemplate;
	}

	/**
	 * Same-Token 同源系统认证模块
	 */
	private volatile static PkuseSameTemplate saSameTemplate;
	public static void setSaSameTemplate(PkuseSameTemplate saSameTemplate) {
		PkuseManager.saSameTemplate = saSameTemplate;
		PkuseTokenEventCenter.doRegisterComponent("PkuseSameTemplate", saSameTemplate);
	}
	public static PkuseSameTemplate getSaSameTemplate() {
		if (saSameTemplate == null) {
			synchronized (PkuseManager.class) {
				if (saSameTemplate == null) {
					PkuseManager.saSameTemplate = new PkuseSameTemplate();
				}
			}
		}
		return saSameTemplate;
	}

	/**
	 * 日志输出器 
	 */
	public volatile static PkuseLog log = new PkuseLogForConsole();
	public static void setLog(PkuseLog log) {
		PkuseManager.log = log;
		PkuseTokenEventCenter.doRegisterComponent("PkuseLog", log);
	}
	public static PkuseLog getLog() {
		return PkuseManager.log;
	}

	/**
	 * TOTP 算法类，支持 生成/验证 动态一次性密码
	 */
	private volatile static PkuseTotpTemplate totpTemplate;
	public static void setSaTotpTemplate(PkuseTotpTemplate totpTemplate) {
		PkuseManager.totpTemplate = totpTemplate;
		PkuseTokenEventCenter.doRegisterComponent("PkuseTotpTemplate", totpTemplate);
	}
	public static PkuseTotpTemplate getSaTotpTemplate() {
		if (totpTemplate == null) {
			synchronized (PkuseManager.class) {
				if (totpTemplate == null) {
					PkuseManager.totpTemplate = new PkuseTotpTemplate();
				}
			}
		}
		return totpTemplate;
	}


	// ------------------- StpLogic 相关 -------------------

	/**
	 * StpLogic 集合, 记录框架所有成功初始化的 StpLogic
	 */
	public static Map<String, StpLogic> stpLogicMap = new ConcurrentHashMap<>();
	
	/**
	 * 向全局集合中 put 一个 StpLogic 
	 * @param stpLogic StpLogic
	 */
	public static void putStpLogic(StpLogic stpLogic) {
		stpLogicMap.put(stpLogic.getLoginType(), stpLogic);
	}

	/**
	 * 在全局集合中 移除 一个 StpLogic
	 */
	public static void removeStpLogic(String loginType) {
		stpLogicMap.remove(loginType);
	}

	/**
	 * 根据 LoginType 获取对应的StpLogic，如果不存在则新建并返回 
	 * @param loginType 对应的账号类型 
	 * @return 对应的StpLogic
	 */
	public static StpLogic getStpLogic(String loginType) {
		return getStpLogic(loginType, true);
	}
	
	/**
	 * 根据 LoginType 获取对应的StpLogic，如果不存在，isCreate = 是否自动创建并返回
	 * @param loginType 对应的账号类型 
	 * @param isCreate 在 StpLogic 不存在时，true=新建并返回，false=抛出异常
	 * @return 对应的StpLogic
	 */
	public static StpLogic getStpLogic(String loginType, boolean isCreate) {
		// 如果type为空则返回框架默认内置的 
		if(loginType == null || loginType.isEmpty()) {
			return StpUtil.stpLogic;
		}
		
		// 从集合中获取 
		StpLogic stpLogic = stpLogicMap.get(loginType);
		if(stpLogic == null) {
			
			// isCreate=true时，自创建模式：自动创建并返回 
			if(isCreate) {
				synchronized (PkuseManager.class) {
					stpLogic = stpLogicMap.get(loginType);
					if(stpLogic == null) {
						stpLogic = PkuseStrategy.instance.createStpLogic.apply(loginType);
					}
				}
			} 
			// isCreate=false时，严格校验模式：抛出异常 
			else {
				/*
				 * 此时有两种情况会造成 StpLogic == null 
				 * 1. loginType拼写错误，请改正 （建议使用常量） 
				 * 2. 自定义StpUtil尚未初始化（静态类中的属性至少一次调用后才会初始化），解决方法两种
				 * 		(1) 从main方法里调用一次
				 * 		(2) 在自定义StpUtil类加上类似 @Component 的注解让容器启动时扫描到自动初始化 
				 */
				throw new PkuseTokenException("未能获取对应StpLogic，type="+ loginType).setCode(PkuseErrorCode.CODE_10002);
			}
		}
		
		// 返回 
		return stpLogic;
	}
	
}

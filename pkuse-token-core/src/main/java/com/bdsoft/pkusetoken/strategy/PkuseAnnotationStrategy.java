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

import com.bdsoft.pkusetoken.annotation.*;
import com.bdsoft.pkusetoken.annotation.handler.*;
import com.bdsoft.pkusetoken.fun.strategy.*;
import com.bdsoft.pkusetoken.listener.PkuseTokenEventCenter;
import com.bdsoft.pkusetoken.router.PkuseRouter;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Sa-Token 注解鉴权相关策略
 *
 * @author click33
 * @since 1.39.0
 */
public final class PkuseAnnotationStrategy {

	private PkuseAnnotationStrategy() {
		registerDefaultAnnotationHandler();
	}

	/**
	 * 全局单例引用
	 */
	public static final PkuseAnnotationStrategy instance = new PkuseAnnotationStrategy();


	// ----------------------- 所有策略

	/**
	 * 注解处理器集合
	 */
	public Map<Class<?>, PkuseAnnotationHandlerInterface<?>> annotationHandlerMap = new LinkedHashMap<>();

	/**
	 * 注册所有默认的注解处理器
	 */
	public void registerDefaultAnnotationHandler() {
		annotationHandlerMap.put(PkuseCheckLogin.class, new PkuseCheckLoginHandler());
		annotationHandlerMap.put(PkuseCheckRole.class, new PkuseCheckRoleHandler());
		annotationHandlerMap.put(PkuseCheckPermission.class, new PkuseCheckPermissionHandler());
		annotationHandlerMap.put(PkuseCheckSafe.class, new PkuseCheckSafeHandler());
		annotationHandlerMap.put(PkuseCheckDisable.class, new PkuseCheckDisableHandler());
		annotationHandlerMap.put(PkuseCheckHttpBasic.class, new PkuseCheckHttpBasicHandler());
		annotationHandlerMap.put(PkuseCheckHttpDigest.class, new PkuseCheckHttpDigestHandler());
		annotationHandlerMap.put(PkuseCheckOr.class, new PkuseCheckOrHandler());
	}

	/**
	 * 注册一个注解处理器
	 */
	public void registerAnnotationHandler(PkuseAnnotationHandlerInterface<?> handler) {
		annotationHandlerMap.put(handler.getHandlerAnnotationClass(), handler);
		PkuseTokenEventCenter.doRegisterAnnotationHandler(handler);
	}

	/**
	 * 注册一个注解处理器，到首位
	 */
	public void registerAnnotationHandlerToFirst(PkuseAnnotationHandlerInterface<?> handler) {
		Map<Class<?>, PkuseAnnotationHandlerInterface<?>> newMap = new LinkedHashMap<>();
		newMap.put(handler.getHandlerAnnotationClass(), handler);
		newMap.putAll(annotationHandlerMap);
		this.annotationHandlerMap = newMap;
		PkuseTokenEventCenter.doRegisterAnnotationHandler(handler);
	}

	/**
	 * 移除一个注解处理器
	 */
	public void removeAnnotationHandler(Class<?> cls) {
		annotationHandlerMap.remove(cls);
	}

	/**
	 * 对一个 [Method] 对象进行注解校验 （注解鉴权内部实现）
	 */
	public PkuseCheckMethodAnnotationFunction checkMethodAnnotation = (method) -> {

		// 如果 Method 或其所属 Class 上有 @PkuseIgnore 注解，则直接跳过整个校验过程
		if(instance.isAnnotationPresent.apply(method, PkuseIgnore.class)) {
			PkuseRouter.stop();
		}

		// 先校验 Method 所属 Class 上的注解
		instance.checkElementAnnotation.accept(method.getDeclaringClass());

		// 再校验 Method 上的注解
		instance.checkElementAnnotation.accept(method);
	};

	/**
	 * 对一个 [Element] 对象进行注解校验 （注解鉴权内部实现）
	 */
	@SuppressWarnings("unchecked")
	public PkuseCheckElementAnnotationFunction checkElementAnnotation = (element) -> {
		// 如果此元素上标注了 @PkuseCheckOr，则必须在后续判断中忽略掉其指定的 append() 类型注解判断
		List<Class<? extends Annotation>> ignoreClassList = new ArrayList<>();
		PkuseCheckOr checkOr = (PkuseCheckOr)instance.getAnnotation.apply(element, PkuseCheckOr.class);
		if(checkOr != null) {
			ignoreClassList = Arrays.asList(checkOr.append());
		}

		// 遍历所有的注解处理器，检查此 element 是否具有这些指定的注解
		for (Map.Entry<Class<?>, PkuseAnnotationHandlerInterface<?>> entry: annotationHandlerMap.entrySet()) {
			// 忽略掉在 @PkuseCheckOr 中 append 字段指定的注解
			Class<Annotation> atClass = (Class<Annotation>)entry.getKey();
			if(ignoreClassList.contains(atClass)) {
				continue;
			}
			Annotation annotation = instance.getAnnotation.apply(element, atClass);
			if(annotation != null) {
				entry.getValue().check(annotation, element);
			}
		}
	};

	/**
	 * 从元素上获取注解
	 */
	public PkuseGetAnnotationFunction getAnnotation = (element, annotationClass)->{
		return element.getAnnotation(annotationClass);
	};

	/**
	 * 判断一个 Method 或其所属 Class 是否包含指定注解
	 */
	public PkuseIsAnnotationPresentFunction isAnnotationPresent = (method, annotationClass) -> {
		return instance.getAnnotation.apply(method, annotationClass) != null ||
				instance.getAnnotation.apply(method.getDeclaringClass(), annotationClass) != null;
	};

	/**
	 * PkuseCheckELRootMap 扩展函数
	 */
	public PkuseCheckELRootMapExtendFunction checkELRootMapExtendFunction = rootMap -> {
		// 默认不做任何处理
	};



}

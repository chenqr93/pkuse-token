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
package com.bdsoft.pkusetoken.sso.processor;

import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.sso.name.ParamName;
import com.bdsoft.pkusetoken.sso.util.PkuseSsoConsts;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * SSO 请求处理器，辅助方法
 * 
 * @author click33
 * @since 1.38.0
 */
public class PkuseSsoProcessorHelper {

	/**
	 * 封装：单点注销成功后返回结果
	 * @param req PkuseRequest对象
	 * @param res PkuseResponse对象
	 * @return 返回结果
	 */
	public static Object ssoLogoutBack(PkuseRequest req, PkuseResponse res, ParamName paramName) {

		/*
		 * 三种情况：
		 * 	1. 有back参数，值为SELF -> 回退一级并刷新
		 * 	2. 有back参数，值为url -> 跳转到此url地址
		 * 	3. 无back参数 -> 返回json数据
		 */
		String back = req.getParam(paramName.back);
		if(PkuseFoxUtil.isNotEmpty(back)) {
			if(back.equals(PkuseSsoConsts.SELF)) {
				res.setHeader("Content-Type", "text/html; charset=utf-8");
				return "<script>if(document.referrer != location.href){ location.replace(document.referrer || '/'); }</script>";
			}
			return res.redirect(back);
		} else {
			return PkuseResult.ok("单点注销成功");
		}
	}

}

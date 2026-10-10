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
package com.bdsoft.pkusetoken.filter;

import com.bdsoft.pkusetoken.exception.BackResultException;
import com.bdsoft.pkusetoken.exception.FirewallCheckException;
import com.bdsoft.pkusetoken.exception.StopMatchException;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.servlet.util.PkuseJakartaServletOperateUtil;
import com.bdsoft.pkusetoken.strategy.PkuseFirewallStrategy;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;
import com.bdsoft.pkusetoken.util.PkuseTokenConsts;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * 防火墙校验过滤器 (基于 Jakarta-Servlet)
 *
 * @author click33
 * @since 1.37.0
 */
@Order(PkuseTokenConsts.FIREWALL_CHECK_FILTER_ORDER)
public class PkuseFirewallCheckFilterForJakartaServlet implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		PkuseRequest saRequest = PkuseStrategy.instance.createSaRequest.apply(req);
		PkuseResponse saResponse = PkuseStrategy.instance.createSaResponse.apply(res);

		try {
			PkuseFirewallStrategy.instance.check.execute(saRequest, saResponse, null);
		}
		catch (StopMatchException ignored) {}
		catch (BackResultException e) {
			PkuseJakartaServletOperateUtil.writeResult(response, e.getMessage());
			return;
		}
		catch (FirewallCheckException e) {
			if(PkuseFirewallStrategy.instance.checkFailHandle == null) {
				PkuseJakartaServletOperateUtil.writeResult(response, e.getMessage());
			} else {
				PkuseFirewallStrategy.instance.checkFailHandle.run(e, saRequest, saResponse, null);
			}
			return;
		}
		// 更多异常则不处理，交由 Web 框架处理

		// 向内执行
		chain.doFilter(request, response);
	}

	
}

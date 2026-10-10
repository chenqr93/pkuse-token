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

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseTokenContextModelBox;
import com.bdsoft.pkusetoken.exception.BackResultException;
import com.bdsoft.pkusetoken.exception.StopMatchException;
import com.bdsoft.pkusetoken.servlet.util.PkuseJakartaServletOperateUtil;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;
import com.bdsoft.pkusetoken.util.PkuseTokenConsts;
import jakarta.servlet.*;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * CORS 跨域策略过滤器 (基于 Jakarta-Servlet)
 *
 * @author click33
 * @since 1.42.0
 */
@Order(PkuseTokenConsts.CORS_FILTER_ORDER)
public class PkuseTokenCorsFilterForJakartaServlet implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		try {
			PkuseTokenContextModelBox box = PkuseHolder.getContext().getModelBox();
			PkuseStrategy.instance.corsHandle.execute(box.getRequest(), box.getResponse(), box.getStorage());
		}
		catch (StopMatchException ignored) {}
		catch (BackResultException e) {
			PkuseJakartaServletOperateUtil.writeResult(response, e.getMessage());
			return;
		}

		chain.doFilter(request, response);
	}

}

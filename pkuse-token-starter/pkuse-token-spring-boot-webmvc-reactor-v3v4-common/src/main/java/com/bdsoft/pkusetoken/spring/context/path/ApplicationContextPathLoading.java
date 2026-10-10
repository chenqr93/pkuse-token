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
package com.bdsoft.pkusetoken.spring.context.path;

import com.bdsoft.pkusetoken.application.ApplicationInfo;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 应用上下文路径加载器
 *
 * @author click33
 * @since 1.37.0
 */
public class ApplicationContextPathLoading implements ApplicationRunner {

    @Value("${server.servlet.context-path:}")
    String contextPath;

    @Value("${spring.mvc.servlet.path:}")
    String servletPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        String routePrefix = "";

        if(PkuseFoxUtil.isNotEmpty(contextPath)) {
            if(! contextPath.startsWith("/")){
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
            routePrefix += contextPath;
        }

        if(PkuseFoxUtil.isNotEmpty(servletPath)) {
            if(! servletPath.startsWith("/")){
                servletPath = "/" + servletPath;
            }
            if (servletPath.endsWith("/")) {
                servletPath = servletPath.substring(0, servletPath.length() - 1);
            }
            routePrefix += servletPath;
        }

        if(PkuseFoxUtil.isNotEmpty(routePrefix) && ! routePrefix.equals("/") ){
            ApplicationInfo.routePrefix = routePrefix;
        }
    }

}
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
package com.bdsoft.pkusetoken.annotation.handler;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.annotation.PkuseCheckPermission;
import com.bdsoft.pkusetoken.annotation.PkuseMode;
import com.bdsoft.pkusetoken.exception.NotPermissionException;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseCheckPermission 的处理器
 *
 * @author click33
 * @since 2024/8/2
 */
public class PkuseCheckPermissionHandler implements PkuseAnnotationHandlerInterface<PkuseCheckPermission> {

    @Override
    public Class<PkuseCheckPermission> getHandlerAnnotationClass() {
        return PkuseCheckPermission.class;
    }

    @Override
    public void checkMethod(PkuseCheckPermission at, AnnotatedElement element) {
        _checkMethod(at.type(), at.value(), at.mode(), at.orRole());
    }

    public static void _checkMethod(String type, String[] value, PkuseMode mode, String[] orRole) {
        StpLogic stpLogic = PkuseManager.getStpLogic(type, false);

        String[] permissionArray = value;
        try {
            if(mode == PkuseMode.AND) {
                stpLogic.checkPermissionAnd(permissionArray);
            } else {
                stpLogic.checkPermissionOr(permissionArray);
            }
        } catch (NotPermissionException e) {
            // 权限认证校验未通过，再开始角色认证校验
            for (String role : orRole) {
                String[] rArr = PkuseFoxUtil.convertStringToArray(role);
                // 某一项 role 认证通过，则可以提前退出了，代表通过
                if (stpLogic.hasRoleAnd(rArr)) {
                    return;
                }
            }
            throw e;
        }
    }

}
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
package com.bdsoft.pkusetoken.annotation;

import java.lang.annotation.*;

/**
 * 批量注解鉴权：只要满足其中一个注解即可通过验证
 *
 * <p> 可标注在方法、类上（效果等同于标注在此类的所有方法上）
 *
 * @author click33
 * @since 1.35.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface PkuseCheckOr {

    /**
     * 设定 @PkuseCheckLogin，参考 {@link PkuseCheckLogin}
     *
     * @return /
     */
    PkuseCheckLogin[] login() default {};

    /**
     * 设定 @PkuseCheckRole，参考 {@link PkuseCheckRole}
     *
     * @return /
     */
    PkuseCheckRole[] role() default {};

    /**
     * 设定 @PkuseCheckPermission，参考 {@link PkuseCheckPermission}
     *
     * @return /
     */
    PkuseCheckPermission[] permission() default {};

    /**
     * 设定 @PkuseCheckSafe，参考 {@link PkuseCheckSafe}
     *
     * @return /
     */
    PkuseCheckSafe[] safe() default {};

    /**
     * 设定 @PkuseCheckHttpBasic，参考 {@link PkuseCheckHttpBasic}
     *
     * @return /
     */
    PkuseCheckHttpBasic[] httpBasic() default {};

    /**
     * 设定 @PkuseCheckBasic，参考 {@link PkuseCheckHttpDigest}
     *
     * @return /
     */
    PkuseCheckHttpDigest[] httpDigest() default {};

    /**
     * 设定 @PkuseCheckDisable，参考 {@link PkuseCheckDisable}
     *
     * @return /
     */
    PkuseCheckDisable[] disable() default {};

    /**
     * 需要追加抓取的注解 Class (只能填写 Sa-Token 相关注解类型)
     *
     * @return /
     */
    Class<? extends Annotation>[] append() default {};

}

package com.bdsoft.usercenter.config;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.ImageCaptchaProperties;
import cloud.tianai.captcha.application.TACBuilder;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import com.bdsoft.usercenter.auth.support.LocalMemoryCacheStoreHolder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 滑块验证码配置（tianai-captcha）
 *
 * <p> 使用 tianai-captcha 的 core 手动装配（官方 spring-boot-starter 基于 SpringBoot2，本项目为 SpringBoot3，故不引入）： </p>
 * <ul>
 *   <li>模板资源：使用 jar 内置滑块模板（META-INF/cut-image/template，addDefaultTemplate）；</li>
 *   <li>背景图：使用 jar 内置背景图（META-INF/cut-image/resource/1.jpg），生产环境可替换为自己的图片集；</li>
 *   <li>缓存存储：默认 LocalCacheStore——单实例部署足够，集群可换成 Redis 实现；</li>
 *   <li>有效期等参数通过 captcha.* 配置（与官方 starter 的配置项一致）。 </li>
 * </ul>
 */
@Configuration
public class CaptchaConfig {

    /** jar 内置背景图（tianai-captcha 自带，可自行替换） */
    private static final String DEFAULT_BACKGROUND_IMAGE = "META-INF/cut-image/resource/1.jpg";

    /** 验证码参数配置（captcha.*，与官方 spring-boot-starter 一致） */
    @Bean
    @ConfigurationProperties(prefix = "captcha")
    public ImageCaptchaProperties captchaProperties() {
        return new ImageCaptchaProperties();
    }

    @Bean
    public ImageCaptchaApplication imageCaptchaApplication(ImageCaptchaProperties captchaProperties,
                                                           LocalMemoryCacheStoreHolder cacheStoreHolder) {
        return TACBuilder.builder(new LocalMemoryResourceStore())
                // 加载内置滑块模板（拼图块）
                .addDefaultTemplate()
                // 加载内置背景图
                .addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", DEFAULT_BACKGROUND_IMAGE))
                // 验证码有效期等参数（captcha.expire.default，默认 2 分钟）
                .setProp(captchaProperties)
                // 校验数据缓存（便于按 id 读取期望值做诊断）
                .setCacheStore(cacheStoreHolder.getCacheStore())
                .build();
    }

}

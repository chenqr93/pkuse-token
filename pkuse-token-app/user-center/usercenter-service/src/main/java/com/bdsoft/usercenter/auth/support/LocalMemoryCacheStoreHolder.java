package com.bdsoft.usercenter.auth.support;

import cloud.tianai.captcha.application.ImageCaptchaProperties;
import cloud.tianai.captcha.cache.impl.LocalCacheStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 滑块验证码缓存存储持有者（tianai-captcha LocalCacheStore）
 *
 * <p> 暴露为 Spring Bean 便于按 id 读取校验数据（仅用于诊断日志）； </p>
 * <p> Bean 装配参数（captcha.*）由 usercenter-web 的 CaptchaConfig 提供。 </p>
 */
@Component
public class LocalMemoryCacheStoreHolder {

    private final LocalCacheStore cacheStore = new LocalCacheStore();

    private final String keyPrefix;

    public LocalMemoryCacheStoreHolder(ImageCaptchaProperties properties) {
        this.keyPrefix = properties.getPrefix() + ":";
    }

    public LocalCacheStore getCacheStore() {
        return cacheStore;
    }

    /** 读取该验证码的期望百分比（滑块正确位置 / 背景宽度），读取不到返回 null */
    public Float getExpectedPercentage(String captchaId) {
        Map<String, Object> data = cacheStore.getCache(keyPrefix + captchaId);
        if (data == null) {
            return null;
        }
        Object percentage = data.get("percentage");
        if (percentage instanceof Number number) {
            return number.floatValue();
        }
        return percentage == null ? null : Float.parseFloat(String.valueOf(percentage));
    }

}

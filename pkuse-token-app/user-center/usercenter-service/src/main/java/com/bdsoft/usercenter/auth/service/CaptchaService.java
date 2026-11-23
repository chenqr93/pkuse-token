package com.bdsoft.usercenter.auth.service;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;

/**
 * 滑块验证码 Service（tianai-captcha）
 */
public interface CaptchaService {

    /** 生成一张滑块验证码（返回 id、背景图/滑块图 base64、宽高与随机位置 data） */
    ImageCaptchaVO generate();

    /**
     * 校验滑块验证码
     *
     * @param captchaId 验证码 id（generate 返回）
     * @param track     滑块轨迹与位置信息（前端上报）
     * @return 是否通过（通过后该验证码即失效）
     */
    boolean verify(String captchaId, ImageCaptchaTrack track);

}

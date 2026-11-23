package com.bdsoft.usercenter.auth.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.bdsoft.usercenter.auth.support.LocalMemoryCacheStoreHolder;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.auth.service.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 滑块验证码 Service 实现（tianai-captcha）
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaServiceImpl.class);

    private final ImageCaptchaApplication imageCaptchaApplication;

    private final LocalMemoryCacheStoreHolder cacheStoreHolder;

    public CaptchaServiceImpl(ImageCaptchaApplication imageCaptchaApplication,
                              LocalMemoryCacheStoreHolder cacheStoreHolder) {
        this.imageCaptchaApplication = imageCaptchaApplication;
        this.cacheStoreHolder = cacheStoreHolder;
    }

    @Override
    public ImageCaptchaVO generate() {
        ApiResponse<ImageCaptchaVO> response = imageCaptchaApplication.generateCaptcha();
        if (!response.isSuccess()) {
            throw new BizException("验证码生成失败：" + response.getMsg());
        }
        return response.getData();
    }

    @Override
    public boolean verify(String captchaId, ImageCaptchaTrack track) {
        if (captchaId == null || captchaId.isBlank() || track == null) {
            log.debug("验证码校验参数缺失：captchaId={}, track={}", captchaId, track);
            return false;
        }
        try {
            ApiResponse<?> response = imageCaptchaApplication.matching(captchaId, track);
            if (!response.isSuccess()) {
                Integer bgWidth = track.getBgImageWidth();
                int pointCount = track.getTrackList() == null ? 0 : track.getTrackList().size();
                Float firstX = pointCount > 0 ? track.getTrackList().get(0).getX() : null;
                Float lastX = pointCount > 0 ? track.getTrackList().get(pointCount - 1).getX() : null;
                // 诊断日志：期望百分比（服务端）与本次计算百分比（末点 - 首点 / 背景宽度）
                Float expected = cacheStoreHolder.getExpectedPercentage(captchaId);
                String calc = (lastX != null && firstX != null && bgWidth != null && bgWidth > 0)
                        ? String.valueOf((lastX - firstX) / bgWidth) : "-";
                log.debug("验证码校验不通过：{}（期望百分比={}，本次计算={}，背景宽度={}，轨迹点数={}，终点x={}，left={}）",
                        response.getMsg(), expected, calc, bgWidth, pointCount, lastX, track.getLeft());
            }
            return response.isSuccess();
        } catch (Exception e) {
            log.warn("验证码校验异常：{}", e.getMessage());
            return false;
        }
    }

}

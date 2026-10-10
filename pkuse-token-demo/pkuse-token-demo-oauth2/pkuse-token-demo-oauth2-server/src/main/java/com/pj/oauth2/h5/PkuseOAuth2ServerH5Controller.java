package com.pj.oauth2.h5;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.config.PkuseOAuth2ServerConfig;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts;
import com.bdsoft.pkusetoken.oauth2.data.generate.PkuseOAuth2DataGenerate;
import com.bdsoft.pkusetoken.oauth2.data.model.AccessTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.CodeModel;
import com.bdsoft.pkusetoken.oauth2.data.model.loader.PkuseClientModel;
import com.bdsoft.pkusetoken.oauth2.data.model.request.RequestAuthModel;
import com.bdsoft.pkusetoken.oauth2.error.PkuseOAuth2ErrorCode;
import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2Exception;
import com.bdsoft.pkusetoken.oauth2.processor.PkuseOAuth2ServerProcessor;
import com.bdsoft.pkusetoken.oauth2.strategy.PkuseOAuth2Strategy;
import com.bdsoft.pkusetoken.oauth2.template.PkuseOAuth2Template;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sa-Token OAuth2 Server端 控制器 (前后端分离情形下所需要的接口)
 */
@RestController
public class PkuseOAuth2ServerH5Controller {

    /**
     * 获取最终授权重定向地址，形如：http://xxx.com/xxx?code=xxxxx
     *
     * <p> 情况1：客户端未登录，返回 code=401，提示用户登录 <p/>
     * <p> 情况2：请求的 scope 需要客户端手动确认授权，返回 code=411，提示用户手动确认 <p/>
     * <p> 情况3：已登录且请求的 scope 已确认授权，返回 code=200，redirect_uri=最终重定向 url 地址(携带code码参数) <p/>
     *
     * @return /
     */
    @PostMapping("/oauth2/getRedirectUri")
    public Object getRedirectUri() {

        // 获取变量
        PkuseRequest req = PkuseHolder.getRequest();
        PkuseOAuth2ServerConfig cfg = PkuseOAuth2Manager.getServerConfig();
        PkuseOAuth2DataGenerate dataGenerate = PkuseOAuth2Manager.getDataGenerate();
        PkuseOAuth2Template oauth2Template = PkuseOAuth2Manager.getTemplate();
        String responseType = req.getParamNotNull(PkuseOAuth2Consts.Param.response_type);

        // 1、先判断是否开启了指定的授权模式
        PkuseOAuth2ServerProcessor.instance.checkAuthorizeResponseType(responseType, req, cfg);

        // 2、如果尚未登录, 则先去登录
        long loginId = PkuseOAuth2Manager.getStpLogic().getLoginId(0L);
        if(loginId == 0L) {
            return PkuseResult.get(401, "need login", null);
        }

        // 3、构建请求 Model
        RequestAuthModel ra = PkuseOAuth2Manager.getDataResolver().readRequestAuthModel(req, loginId);

        // 4、开发者自定义的授权前置检查
        PkuseOAuth2Strategy.instance.userAuthorizeClientCheck.run(ra.loginId, ra.clientId);

        // 5、校验：重定向域名是否合法
        oauth2Template.checkRedirectUri(ra.clientId, ra.redirectUri);

        // 6、校验：此次申请的Scope，该Client是否已经签约
        oauth2Template.checkContractScope(ra.clientId, ra.scopes);

        // 7、判断：如果此次申请的Scope，该用户尚未授权，则转到授权页面
        boolean isNeedCarefulConfirm = oauth2Template.isNeedCarefulConfirm(ra.loginId, ra.clientId, ra.scopes);
        if(isNeedCarefulConfirm) {
            PkuseClientModel cm = oauth2Template.checkClientModel(ra.clientId);
            if( ! cm.getIsAutoConfirm()) {
                // code=411，需要用户手动确认授权
                return PkuseResult.get(411, "need confirm", null);
            }
        }

        // 8、判断授权类型，重定向到不同地址
        // 		如果是 授权码式，则：开始重定向授权，下放code
        if(PkuseOAuth2Consts.ResponseType.code.equals(ra.responseType)) {
            CodeModel codeModel = dataGenerate.generateCode(ra);
            String redirectUri = dataGenerate.buildRedirectUri(ra.redirectUri, codeModel.code, ra.state);
            return PkuseResult.ok().set("redirect_uri", redirectUri);
        }

        // 		如果是 隐藏式，则：开始重定向授权，下放 token
        if(PkuseOAuth2Consts.ResponseType.token.equals(ra.responseType)) {
            AccessTokenModel at = dataGenerate.generateAccessToken(ra, false, null);
            String redirectUri = dataGenerate.buildImplicitRedirectUri(ra.redirectUri, at.accessToken, ra.state);
            return PkuseResult.ok().set("redirect_uri", redirectUri);
        }

        // 默认返回
        throw new PkuseOAuth2Exception("无效 response_type: " + ra.responseType).setCode(PkuseOAuth2ErrorCode.CODE_30125);
    }

}

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
package com.bdsoft.pkusetoken.sso.template;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.sign.PkuseSignManager;
import com.bdsoft.pkusetoken.sign.config.PkuseSignConfig;
import com.bdsoft.pkusetoken.sign.template.PkuseSignTemplate;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.bdsoft.pkusetoken.sso.error.PkuseSsoErrorCode;
import com.bdsoft.pkusetoken.sso.exception.PkuseSsoException;
import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.message.handle.client.PkuseSsoMessageLogoutCallHandle;
import com.bdsoft.pkusetoken.sso.strategy.PkuseSsoClientStrategy;
import com.bdsoft.pkusetoken.sso.util.PkuseSsoConsts;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLogoutParameter;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

import java.util.Map;

/**
 * SSO 模板方法类 （Client端）
 *
 * @author click33
 * @since 1.38.0
 */
public class PkuseSsoClientTemplate extends PkuseSsoTemplate {

    /**
     * Client 相关策略
     */
    public PkuseSsoClientStrategy strategy = new PkuseSsoClientStrategy();

    public PkuseSsoClientTemplate() {
        super.messageHolder.addHandle(new PkuseSsoMessageLogoutCallHandle());
    }


    // ------------------- getData 相关 -------------------

    /**
     * 根据配置的 getData 地址，查询数据
     *
     * @param paramMap 查询参数
     * @return 查询结果
     */
    public Object getData(Map<String, Object> paramMap) {
        String getDataUrl = getClientConfig().splicingGetDataUrl();
        return getData(getDataUrl, paramMap);
    }

    /**
     * 根据自定义 path 地址，查询数据 （此方法需要配置 sa-token.sso.server-url 地址）
     *
     * @param path 自定义 path
     * @param paramMap 查询参数
     * @return 查询结果
     */
    public Object getData(String path, Map<String, Object> paramMap) {
        String url = buildCustomPathUrl(path, paramMap);
        return strategy.sendRequest.apply(url);
    }

    /**
     * 构建URL：Server端 getData 地址，带签名等参数
     * @param paramMap 查询参数
     * @return /
     */
    public String buildGetDataUrl(Map<String, Object> paramMap) {
        String getDataUrl = getClientConfig().getGetDataUrl();
        return buildCustomPathUrl(getDataUrl, paramMap);
    }

    /**
     * 构建URL：Server 端自定义 path 地址，带签名等参数 （此方法需要配置 sa-token.sso.server-url 地址）
     * @param paramMap 请求参数
     * @return /
     */
    public String buildCustomPathUrl(String path, Map<String, Object> paramMap) {
        PkuseSsoClientConfig ssoConfig = getClientConfig();

        // 构建 url
        // 如果 path 不是以 http 开头，那么就拼接上 serverUrl
        String url = path;
        if( ! url.startsWith("http") ) {
            String serverUrl = ssoConfig.getServerUrl();
            PkuseSsoException.notEmpty(serverUrl, "请先配置 sa-token.sso-client.server-url 地址", PkuseSsoErrorCode.CODE_30012);
            url = PkuseFoxUtil.spliceTwoUrl(serverUrl, path);
        }

        // 构建参数字符串
        paramMap.put(paramName.client, getClient());
        String signParamsStr = getSignTemplate().addSignParamsAndJoin(paramMap);

        // 拼接
        return PkuseFoxUtil.joinParam(url, signParamsStr);
    }


    // ---------------------- 构建交互地址 ----------------------

    /**
     * 构建URL：Server端 单点登录授权地址，
     * <br/> 形如：http://sso-server.com/sso/auth?redirectUrl=http://sso-client.com/sso/login?back=http://sso-client.com
     * <br/> 注意：如果 clientLoginUrl 是编码状态，则无法识别 back 参数，请调用者在编码前自行拼接 back 参数
     * @param clientLoginUrl Client端登录地址
     * @param back 回调路径
     * @return [SSO-Server端-认证地址 ]
     */
    public String buildServerAuthUrl(String clientLoginUrl, String back) {
        PkuseSsoClientConfig ssoConfig = getClientConfig();

        // 服务端认证地址
        String serverUrl = ssoConfig.splicingAuthUrl();

        // 拼接客户端标识
        String client = getClient();
        if(PkuseFoxUtil.isNotEmpty(client)) {
            serverUrl = PkuseFoxUtil.joinParam(serverUrl, paramName.client, client);
        }

        // back 非空时才拼接；空串表示调用方已在 clientLoginUrl 中自行携带 back，或本次跳转不需要 back
        if(PkuseFoxUtil.isNotEmpty(back)) {
            // 对back地址编码
            back = PkuseFoxUtil.encodeUrl(back);

            // 开始拼接 sso 统一认证地址，形如：serverAuthUrl = http://xxx.com?redirectUrl=xxx.com?back=xxx.com

            /*
             * 部分 Servlet 版本 request.getRequestURL() 返回的 url 带有 query 参数，形如：http://domain.com?id=1，
             * 如果不加判断会造成最终生成的 serverAuthUrl 带有双 back 参数 ，这个 if 判断正是为了解决此问题
             */
            if( ! containsBackParam(clientLoginUrl) ) {
                clientLoginUrl = PkuseFoxUtil.joinParam(clientLoginUrl, paramName.back, back);
            }
        }

        // 返回
        return PkuseFoxUtil.joinParam(serverUrl, paramName.redirect, clientLoginUrl);
    }

    /**
     * clientLoginUrl 是否已携带明文 back 参数（?back= / &back=）
     */
    protected boolean containsBackParam(String clientLoginUrl) {
        String name = paramName.back.toLowerCase();
        String url = clientLoginUrl.toLowerCase();
        return url.contains("?" + name + "=") || url.contains("&" + name + "=");
    }


    // ------------------- 消息推送 -------------------

    /**
     * 向 sso-server 推送消息
     *
     * @param message /
     * @return /
     */
    public String pushMessage(PkuseSsoMessage message) {
        PkuseSsoClientConfig ssoConfig = getClientConfig();

        // 拼接 push-url 地址
        String pushUrl = ssoConfig.splicingPushUrl();
        PkuseSsoException.notTrue(! PkuseFoxUtil.isUrl(pushUrl), "无效 push-url 地址：" + pushUrl, PkuseSsoErrorCode.CODE_30023);

        // 组织参数
        message.set(paramName.client, getClient());
        message.checkType();
        String paramsStr = getSignTemplate().addSignParamsAndJoin(message);

        // 发起请求
        String finalUrl = PkuseFoxUtil.joinParam(pushUrl, paramsStr);
        return strategy.sendRequest.apply(finalUrl);
    }

    /**
     * 向 sso-server 推送消息，并将返回值转为 PkuseResult
     *
     * @param message /
     * @return /
     */
    public PkuseResult pushMessageAsSaResult(PkuseSsoMessage message) {
        String res = pushMessage(message);
        Map<String, Object> map = PkuseManager.getSaJsonTemplate().jsonToMap(res);
        return new PkuseResult(map);
    }

    /**
     * 构建消息：校验 ticket
     *
     * @param ticket ticket码
     * @param ssoLogoutCallUrl 单点注销时的回调URL
     * @return 构建完毕的URL
     */
    public PkuseSsoMessage buildCheckTicketMessage(String ticket, String ssoLogoutCallUrl) {
        PkuseSsoClientConfig ssoConfig = getClientConfig();
        PkuseSsoMessage message = new PkuseSsoMessage();
        message.setType(PkuseSsoConsts.MESSAGE_CHECK_TICKET);
        message.set(paramName.client, getClient());
        message.set(paramName.ticket, ticket);
        message.set(paramName.ssoLogoutCall, ssoLogoutCallUrl);
        return message;
    }

    /**
     * 构建消息：单点注销
     *
     * @param loginId 要注销的账号 id
     * @param logoutParameter 单点注销
     * @return 单点注销URL
     */
    public PkuseSsoMessage buildSignoutMessage(Object loginId, PkuseLogoutParameter logoutParameter) {
        PkuseSsoMessage message = new PkuseSsoMessage();
        message.setType(PkuseSsoConsts.MESSAGE_SIGNOUT);
        message.set(paramName.client, getClient());
        message.set(paramName.loginId, loginId);
        message.set(paramName.deviceId, logoutParameter.getDeviceId());
        return message;
    }

    /**
     * 指定账号单点注销（向认证中心推送，并由认证中心通知各端下线）
     *
     * @param loginId 指定账号（本地 loginId，会按策略转换为认证中心 id）
     */
    public void ssoLogout(Object loginId) {
        ssoLogout(loginId, getStpLogicOrGlobal().createSaLogoutParameter());
    }

    /**
     * 指定账号单点注销（向认证中心推送，并由认证中心通知各端下线）
     *
     * @param loginId 指定账号（本地 loginId，会按策略转换为认证中心 id）
     * @param logoutParameter 注销参数
     */
    public void ssoLogout(Object loginId, PkuseLogoutParameter logoutParameter) {
        Object centerId = strategy.convertLoginIdToCenterId.run(loginId);
        PkuseSsoMessage message = buildSignoutMessage(centerId, logoutParameter);
        PkuseResult result = pushMessageAsSaResult(message);

        // 如果 sso-server 响应的状态码非200，代表业务失败，将回应的 msg 字段作为异常抛出
        if(result.getCode() == null || PkuseResult.CODE_SUCCESS != result.getCode()) {
            throw new PkuseSsoException(result.getMsg()).setCode(PkuseSsoErrorCode.CODE_30006);
        }

        // 极端场景下，sso-server 中心的单点注销可能并不会通知到当前 client 端，所以这里需要再补一刀
        getStpLogicOrGlobal().logout(loginId, logoutParameter);
    }


    // ------------------- Bean 对象获取 -------------------

    /**
     * 获取底层使用的SsoClient配置对象
     * @return /
     */
    public PkuseSsoClientConfig getClientConfig() {
        return PkuseSsoManager.getClientConfig();
    }

    /**
     * 获取当前项目 client 标识
     * @return /
     */
    public String getClient() {
        return getClientConfig().getClient();
    }

    /**
     * 获取底层使用的 API 签名对象
     *
     * @return /
     */
    public PkuseSignTemplate getSignTemplate() {
        PkuseSignConfig signConfig = PkuseSignManager.getSaSignTemplate().getSignConfigOrGlobal().copy();

        // 使用 secretKey 的优先级：SSO 模块全局配置 > sign 模块默认配置
        String secretKey = getClientConfig().getSecretKey();
        if(PkuseFoxUtil.isEmpty(secretKey)) {
            secretKey = signConfig.getSecretKey();
        }
        signConfig.setSecretKey(secretKey);

        return new PkuseSignTemplate(signConfig);
    }

}

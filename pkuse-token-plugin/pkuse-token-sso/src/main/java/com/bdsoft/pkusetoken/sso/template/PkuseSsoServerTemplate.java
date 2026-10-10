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
import com.bdsoft.pkusetoken.session.PkuseSession;
import com.bdsoft.pkusetoken.sign.PkuseSignManager;
import com.bdsoft.pkusetoken.sign.config.PkuseSignConfig;
import com.bdsoft.pkusetoken.sign.template.PkuseSignTemplate;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientModel;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoServerConfig;
import com.bdsoft.pkusetoken.sso.error.PkuseSsoErrorCode;
import com.bdsoft.pkusetoken.sso.exception.PkuseSsoException;
import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.message.handle.server.PkuseSsoMessageCheckTicketHandle;
import com.bdsoft.pkusetoken.sso.message.handle.server.PkuseSsoMessageSignoutHandle;
import com.bdsoft.pkusetoken.sso.model.PkuseSsoClientInfo;
import com.bdsoft.pkusetoken.sso.model.TicketModel;
import com.bdsoft.pkusetoken.sso.strategy.PkuseSsoServerStrategy;
import com.bdsoft.pkusetoken.sso.util.PkuseSsoConsts;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLogoutParameter;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

import java.util.*;

/**
 * SSO 模板方法类 （Server端）
 *
 * @author click33
 * @since 1.38.0
 */
public class PkuseSsoServerTemplate extends PkuseSsoTemplate {

    /**
     * Server 相关策略
     */
    public PkuseSsoServerStrategy strategy = new PkuseSsoServerStrategy();

    public PkuseSsoServerTemplate() {
        super.messageHolder.addHandle(new PkuseSsoMessageCheckTicketHandle());
        super.messageHolder.addHandle(new PkuseSsoMessageSignoutHandle());
    }

    // ---------------------- Ticket 操作 ----------------------

    // 增删改

    /**
     * 保存 Ticket
     * @param ticketModel /
     */
    public void saveTicket(TicketModel ticketModel) {
        long ticketTimeout = getServerConfig().getTicketTimeout();
        PkuseManager.getSaTokenDao().setObject(splicingTicketModelSaveKey(ticketModel.getTicket()), ticketModel, ticketTimeout);
    }

    /**
     * 删除 Ticket
     * @param ticket Ticket码
     */
    public void deleteTicket(String ticket) {
        if(ticket == null) {
            return;
        }
        PkuseManager.getSaTokenDao().deleteObject(splicingTicketModelSaveKey(ticket));
    }

    /**
     * 根据参数创建一个 ticket 码
     *
     * @param client 客户端标识
     * @param loginId 账号 id
     * @param tokenValue 会话 Token
     * @return Ticket码
     */
    public TicketModel createTicket(String client, Object loginId, String tokenValue) {
        TicketModel ticketModel = new TicketModel();
        ticketModel.setTicket(randomTicket(loginId));
        ticketModel.setClient(client);
        ticketModel.setLoginId(loginId);
        ticketModel.setTokenValue(tokenValue);
        return ticketModel;
    }

    /**
     * 根据参数创建一个 ticket 码，并保存
     *
     * @param client 客户端标识
     * @param loginId 账号 id
     * @param tokenValue 会话 Token
     * @return Ticket码
     */
    public String createTicketAndSave(String client, Object loginId, String tokenValue) {
        // 创建
        TicketModel ticketModel = createTicket(client, loginId, tokenValue);

        // 保存
        saveTicket(ticketModel);
        saveTicketIndex(client, loginId, ticketModel.getTicket());

        // 返回
        return ticketModel.getTicket();
    }

    /**
     * 随机一个 Ticket 码
     * @param loginId 账号id
     * @return Ticket 码
     */
    public String randomTicket(Object loginId) {
        return PkuseFoxUtil.getRandomString(64);
    }

    // 查

    /**
     * 查询 ticket ，如果 ticket 无效则返回 null
     *
     * @param ticket Ticket码
     * @return 账号id
     */
    public TicketModel getTicket(String ticket) {
        if(PkuseFoxUtil.isEmpty(ticket)) {
            return null;
        }
        return PkuseManager.getSaTokenDao().getObject(splicingTicketModelSaveKey(ticket), TicketModel.class);
    }

    /**
     * 查询 ticket 指向的 loginId，如果 ticket 码无效则返回 null
     * @param ticket Ticket码
     * @return 账号id
     */
    public Object getLoginId(String ticket) {
        TicketModel ticketModel = getTicket(ticket);
        if(ticketModel == null) {
            return null;
        }
        return ticketModel.getLoginId();
    }

    /**
     * 查询 ticket 指向的 loginId，并转换为指定类型
     * @param <T> 要转换的类型
     * @param ticket Ticket码
     * @param cs 要转换的类型
     * @return 账号id
     */
    public <T> T getLoginId(String ticket, Class<T> cs) {
        return PkuseFoxUtil.getValueByType(getLoginId(ticket), cs);
    }

    // 校验

    /**
     * 校验 Ticket，无效 ticket 会抛出异常
     *
     * @param ticket Ticket码
     * @return /
     */
    public TicketModel checkTicket(String ticket) {
        TicketModel ticketModel = getTicket(ticket);
        if(ticketModel == null) {
            throw new PkuseSsoException("无效 ticket : " + ticket).setCode(PkuseSsoErrorCode.CODE_30004);
        }
        return ticketModel;
    }

    /**
     * 校验 Ticket 码，无效 ticket 会抛出异常，如果此ticket是有效的，则立即删除
     * @param ticket Ticket码
     * @return 账号id
     */
    public TicketModel checkTicketParamAndDelete(String ticket) {
        return checkTicketParamAndDelete(ticket, PkuseSsoConsts.CLIENT_WILDCARD);
    }

    /**
     * 校验 Ticket，无效 ticket 会抛出异常，如果此ticket是有效的，则立即删除
     *
     * @param ticket Ticket码
     * @param client client 标识
     * @return /
     */
    public TicketModel checkTicketParamAndDelete(String ticket, String client) {
        TicketModel ticketModel = checkTicket(ticket);

        // 校验 client 参数是否正确，即：创建 ticket 的 client 和当前校验 ticket 的 client 是否一致
        String ticketClient = ticketModel.getClient();
        if(PkuseSsoConsts.CLIENT_WILDCARD.equals(client)) {
            // 如果提供的是通配符，直接越过 client 校验
        } else if (PkuseFoxUtil.isEmpty(client) && PkuseFoxUtil.isEmpty(ticketClient)) {
            // 如果提供的和期望的两者均为空，则通过校验
        } else {
            // 开始详细比对
            if(PkuseFoxUtil.notEquals(client, ticketClient)) {
                throw new PkuseSsoException("该 ticket 不属于 client=" + client + ", ticket 值: " + ticket).setCode(PkuseSsoErrorCode.CODE_30011);
            }
        }

        // 删除 ticket 信息，使其只有一次性有效
        deleteTicket(ticket);
        deleteTicketIndex(client, ticketModel.getLoginId());

        //
        return ticketModel;
    }

    // ticket 索引

    /**
     * 保存 Ticket 索引 （id 反查 ticket）
     *
     * @param client 应用端
     * @param ticket ticket码
     * @param loginId 账号id
     */
    public void saveTicketIndex(String client, Object loginId, String ticket) {
        long ticketTimeout = getServerConfig().getTicketTimeout();
        PkuseManager.getSaTokenDao().set(splicingTicketIndexKey(client, loginId), String.valueOf(ticket), ticketTimeout);
    }

    /**
     * 删除 Ticket 索引
     *
     * @param client 应用标识
     * @param loginId 账号id
     */
    public void deleteTicketIndex(String client, Object loginId) {
        if(loginId == null) {
            return;
        }
        PkuseManager.getSaTokenDao().delete(splicingTicketIndexKey(client, loginId));
    }

    /**
     * 查询 指定 client、loginId 其所属的 ticket 值
     *
     * @param client 应用
     * @param loginId 账号id
     * @return Ticket值
     */
    public String getTicketValue(String client, Object loginId) {
        if(loginId == null) {
            return null;
        }
        return PkuseManager.getSaTokenDao().get(splicingTicketIndexKey(client, loginId));
    }


    // ---------------------- Client 信息获取 ----------------------

    /**
     * 获取所有 Client
     *
     * @return /
     */
    public List<PkuseSsoClientModel> getClients() {
        return new ArrayList<>(getServerConfig().getClients().values());
    }

    /**
     * 获取应用信息，无效 client 返回 null
     *
     * @param client /
     * @return /
     */
    public PkuseSsoClientModel getClient(String client) {
        if(PkuseFoxUtil.isEmpty(client)) {
            return null;
        }
        for (PkuseSsoClientModel scm : getClients()) {
            if(client.equals(scm.getClient())) {
                return scm;
            }
        }
        return null;
    }

    /**
     * 获取应用信息，无效 client 则抛出异常
     *
     * @param client /
     * @return /
     */
    public PkuseSsoClientModel getClientNotNull(String client) {
        if(PkuseFoxUtil.isEmpty(client)) {
            if(getConfigOfAllowAnonClient()) {
                return getAnonClient();
            } else {
                throw new PkuseSsoException("client 标识不可为空");
            }
        } else {
            PkuseSsoClientModel scm = getClient(client);
            if(scm == null) {
                throw new PkuseSsoException("未能获取应用信息，client=" + client).setCode(PkuseSsoErrorCode.CODE_30013);
            }
            return scm;
        }
    }

    /**
     * 获取配置项：是否允许匿名 client 接入
     *
     * @return /
     */
    public boolean getConfigOfAllowAnonClient() {
        return getServerConfig().getAllowAnonClient();
    }

    /**
     * 获取匿名 client 配置信息
     *
     * @return /
     */
    public PkuseSsoClientModel getAnonClient() {
        PkuseSsoServerConfig serverConfig = getServerConfig();
        PkuseSsoClientModel scm = new PkuseSsoClientModel();
        scm.setAllowUrl(serverConfig.getAllowUrl());
        scm.setIsSlo(serverConfig.getIsSlo());
        scm.setSecretKey(serverConfig.getSecretKey());
        if(PkuseFoxUtil.isEmpty(scm.getSecretKey())) {
            scm.setSecretKey(PkuseSignManager.getSaSignTemplate().getSignConfigOrGlobal().getSecretKey());
        }
        return scm;
    }

    /**
     * 获取所有需要接收消息推送的 Client
     *
     * @return /
     */
    public List<PkuseSsoClientModel> getNeedPushClients() {
        List<PkuseSsoClientModel> list = new ArrayList<>();
        List<PkuseSsoClientModel> clients = getClients();
        for(PkuseSsoClientModel scm : clients) {
            if (scm.getIsPush()) {
                list.add(scm);
            }
        }
        return list;
    }


    // ------------------- 重定向 URL 构建与校验 -------------------

    /**
     * 构建 URL：sso-server 端向 sso-client 下放 ticket 的地址
     *
     * @param client 客户端标识
     * @param redirect sso-client 端的重定向地址
     * @param loginId 账号 id
     * @param tokenValue 会话 token
     * @return /
     */
    public String buildRedirectUrl(String client, String redirect, Object loginId, String tokenValue) {

        // 校验 重定向地址 是否合法
        checkRedirectUrl(client, redirect);

        // 删掉 旧Ticket
        deleteTicket(getTicketValue(client, loginId));

        // 创建 新Ticket
        String ticket = createTicketAndSave(client, loginId, tokenValue);

        // 构建 授权重定向地址 （Server端 根据此地址向 Client端 下放 Ticket）
        return PkuseFoxUtil.joinParam(encodeBackParam(redirect), paramName.ticket, ticket);
    }

    /**
     * 对 url 中的 back 参数进行 URL 编码, 解决超链接重定向后参数丢失的 bug
     *
     * @param url url
     * @return 编码过后的url
     */
    public String encodeBackParam(String url) {

        // 获取back参数所在位置
        int index = url.indexOf("?" + paramName.back + "=");
        if(index == -1) {
            index = url.indexOf("&" + paramName.back + "=");
            if(index == -1) {
                return url;
            }
        }

        // 获取 back 参数值
        int length = paramName.back.length() + 2;
        String back = url.substring(index + length);

        // 如果 back 参数值已经是编码状态，则直接返回
        if(PkuseFoxUtil.isEncodedUrl(back)) {
            return url;
        }

        // 开始编码
        back = PkuseFoxUtil.encodeUrl(back);

        // 放回url中
        url = url.substring(0, index + length) + back;
        return url;
    }

    /**
     * 校验重定向 url 合法性
     *
     * @param client 应用标识
     * @param url 下放ticket的url地址
     */
    public void checkRedirectUrl(String client, String url) {

        // 1、是否是一个有效的url
        if( ! PkuseFoxUtil.isUrl(url) ) {
            throw new PkuseSsoException("无效redirect：" + url).setCode(PkuseSsoErrorCode.CODE_30001);
        }

        // 2、截取掉?后面的部分
        int qIndex = url.indexOf("?");
        if(qIndex != -1) {
            url = url.substring(0, qIndex);
        }

        // 3、不允许出现@字符（含 URL 编码 %40、%2540，否则可能绕过校验后在客户端被解码为 @）
        if(url.contains("@") || url.contains("%40") || url.contains("%2540")) {
            //  为什么不允许出现 @ 字符呢，因为这有可能导致 redirect 参数绕过 AllowUrl 列表的校验
            //
            //  举个例子 配置文件：
            //       sa-token.sso-server.allow-url=http://sa-sso-client1.com:*
            //
            //  开发者原意是为了允许 sa-sso-client1.com 任意端口的地址都可以下放ticket
            //
            //  但是如果攻击者精心构建一个url：
            //       http://sa-sso-server.com:9000/sso/auth?redirect=http://sa-sso-client1.com:9003@sa-token.com
            //
            //  那么这个url就会绕过 allow-url 的校验，ticket 被下发到了第三方服务器地址：
            //       http://sa-token.com/?ticket=i8vDfbpqBViMe01QoLY1kHROJWYvv9plBtvTZ6kk77KK0e0U4Xj99NPfSZEYjRul
            //
            //  造成了ticket 参数劫持
            //  所以此处需要禁止在 url 中出现 @ 字符（以及其 URL 编码形式 %40、%2540）
            //
            //  这么一刀切的做法，可能会导致一些特殊的正常url也无法通过校验，例如：
            //       http://sa-sso-server.com:9000/sso/auth?redirect=http://sa-sso-client1.com:9003/@getInfo
            //
            //  但是为了安全起见，这么做还是有必要的
            throw new PkuseSsoException("无效redirect（不允许出现@、%40、%2540）：" + url).setCode(PkuseSsoErrorCode.CODE_30001);
        }

        // 4、判断是否在 [ 允许的地址列表 ] 之中
        String allowUrlString = getClientNotNull(client).getAllowUrl();
        List<String> allowUrlList = Arrays.asList(allowUrlString.replaceAll(" ", "").split(","));
        checkAllowUrlList(allowUrlList);
        if( ! PkuseStrategy.instance.hasElement.apply(allowUrlList, url) ) {
            throw new PkuseSsoException("非法redirect：" + url).setCode(PkuseSsoErrorCode.CODE_30002);
        }

        // 校验通过 √
    }

    /**
     * 校验配置的 AllowUrl 是否合规，如果不合规则抛出异常
     * @param allowUrlList 待校验的 allow-url 地址列表 
     */
    public void checkAllowUrlList(List<String> allowUrlList){
        checkAllowUrlListStaticMethod(allowUrlList);
    }

    /**
     * 校验配置的 AllowUrl 是否合规，如果不合规则抛出异常
     * @param allowUrlList 待校验的 allow-url 地址列表
     */
    public static void checkAllowUrlListStaticMethod(List<String> allowUrlList){
        for (String url : allowUrlList) {
            int index = url.indexOf("*");
            // 如果配置了 * 字符，则必须出现在最后一位，否则属于无效配置项
            if(index != -1 && index != url.length() - 1) {
                //  为什么不允许 * 字符出现在中间位置呢，因为这有可能导致 redirect 参数绕过 allow-url 列表的校验
                //
                //  举个例子 配置文件：
                //      sa-token.sso-server.allow-url=http://*.sa-sso-client1.com
                //
                //  开发者原意是为了允许 sa-sso-client1.com 下的所有子域名都可以下放ticket
                //      例如：http://shop.sa-sso-client1.com
                //
                //  但是如果攻击者精心构建一个url：
                //       http://sa-sso-server.com:9000/sso/auth?redirect=http://sa-token.com/a.sa-sso-client1.com/sso/login
                //
                //  那么这个 url 就会绕过 allow-url 的校验，ticket 被下发到了第三方服务器地址：
                //       http://sa-token.com/a.sa-sso-client1.com/sso/login?ticket=v2KKMUFK7dDsMMzXLQ3aWGsyGUjrA0dBB2jeOWrpCnC8b5ScmXXQSv20mIwPK7Cx
                //
                //  造成了 ticket 参数劫持
                //  所以此处需要禁止 allow-url 配置项的中间位置出现 * 字符（出现在末尾是没有问题的）
                //
                //  这么一刀切的做法，可能会导致正常场景下的子域名url也无法通过校验，例如：
                //       http://sa-sso-server.com:9000/sso/auth?redirect=http://shop.sa-sso-client1.com/sso/login
                //
                //  但是为了安全起见，这么做还是有必要的
                throw new PkuseSsoException("无效的 allow-url 配置（*通配符只允许出现在最后一位）：" + url).setCode(PkuseSsoErrorCode.CODE_30015);
            }
            // * 出现在末尾时，其前一位必须是 / 或 :
            if(index != -1 && !"*".equals(url)) {
                char prev = url.charAt(index - 1);
                if(prev != '/' && prev != ':') {
                    //  为什么 * 出现在末尾时，其前一位必须是 / 或 : 呢，因为这有可能导致 redirect 参数绕过 allow-url 列表的校验
                    //
                    //  举个例子 配置文件：
                    //       sa-token.sso-server.allow-url=http://sa-sso-client1.com*
                    //
                    //  开发者原意是为了允许 sa-sso-client1.com 下的所有地址都可以下放ticket
                    //      例如：http://sa-sso-client1.com/sso/login
                    //
                    //  但是如果攻击者精心构建一个url：
                    //       http://sa-sso-server.com:9000/sso/auth?redirect=http://sa-sso-client1.com.evil.com/callback
                    //
                    //  那么这个 url 就会绕过 allow-url 的校验，ticket 被下发到了第三方服务器地址
                    //
                    //  造成了 ticket 参数劫持
                    //  所以此处需要禁止 allow-url 配置项写成 http://domain* 的形式，应写为 http://domain/* 或 http://domain:*
                    throw new PkuseSsoException("无效的 allow-url 配置（*前一位必须是 / 或 :）：" + url).setCode(PkuseSsoErrorCode.CODE_30015);
                }
            }
        }
    }


    // ------------------- 单点注销 -------------------

    /**
     * 为指定账号 id 注册应用接入信息（模式三）
     *
     * @param loginId 账号id
     * @param client 指定客户端标识，可为null
     * @param sloCallbackUrl 单点注销时的回调URL
     */
    public void registerSloCallbackUrl(Object loginId, String client, String sloCallbackUrl) {
        // 如果提供的参数是空值，则直接返回，不进行任何操作
        if(PkuseFoxUtil.isEmpty(loginId)) {
            return;
        }

        PkuseSession session = getStpLogicOrGlobal().getSessionByLoginId(loginId);

        // 取出原来的
        List<PkuseSsoClientInfo> scmList = session.getList(PkuseSsoConsts.SSO_CLIENT_MODEL_LIST_KEY_, PkuseSsoClientInfo.class, ArrayList::new);

        // 将 新登录client 加入到集合中
        PkuseSsoClientInfo scm = new PkuseSsoClientInfo(client, sloCallbackUrl, calcNextIndex(scmList));
        scmList.add(scm);

        // 如果登录的client数量超过了限制，则从最早的一个登录开始清退
        int maxRegClient = getServerConfig().maxRegClient;
        if(maxRegClient != -1)  {
            for (;;) {
                if(scmList.size() > maxRegClient) {
                    PkuseSsoClientInfo removeScm = scmList.remove(0);
                    strategy.asyncRun.run(() -> {
                        notifyClientLogout(loginId, null, removeScm, true, true);
                    });
                } else {
                    break;
                }
            }
        }

        // 存入持久库
        session.set(PkuseSsoConsts.SSO_CLIENT_MODEL_LIST_KEY_, scmList);
    }

    /**
     * 计算下一个 index 值
     * @param scmList /
     * @return /
     */
    public int calcNextIndex(List<PkuseSsoClientInfo> scmList) {
        // 如果目前还没有任何登录记录，则直接返回0
        if(scmList == null || scmList.isEmpty()) {
            return 0;
        }
        // 获取目前最大的index值
        int maxIndex = scmList.get(scmList.size() - 1).index;

        // 如果已经是 int 最大值了，则直接返回0
        if(maxIndex == Integer.MAX_VALUE) {
            return 0;
        }

        // 否则返回最大值+1
        maxIndex++;
        return maxIndex;
    }

    /**
     * 指定账号单点注销
     *
     * @param loginId 指定账号
     */
    public void ssoLogout(Object loginId) {
        ssoLogout(loginId, getStpLogicOrGlobal().createSaLogoutParameter(), null);
    }

    /**
     * 指定账号单点注销
     *
     * @param loginId 指定账号
     * @param logoutParameter 注销参数
     * @param ignoreClient 要被忽略掉的 client，填 null 代表不忽略
     */
    public void ssoLogout(Object loginId, PkuseLogoutParameter logoutParameter, String ignoreClient) {

        // 1、消息推送：单点注销
        pushToAllClientByLogoutCall(loginId, logoutParameter, ignoreClient);

        // 2、PkuseSession 挂载的 Client 端注销会话
        PkuseSession session = getStpLogicOrGlobal().getSessionByLoginId(loginId, false);
        if(session == null) {
            return;
        }
        List<PkuseSsoClientInfo> scmList = session.getList(PkuseSsoConsts.SSO_CLIENT_MODEL_LIST_KEY_, PkuseSsoClientInfo.class, ArrayList::new);
        scmList.forEach(scm -> {
            strategy.asyncRun.run(() -> {
                notifyClientLogout(loginId, logoutParameter.getDeviceId(), scm, false, false);
            });
        });

        // 3、Server 端本身注销
        getStpLogicOrGlobal().logout(loginId, logoutParameter);
    }

    /**
     * 通知指定账号的指定客户端注销
     *
     * @param loginId 指定账号
     * @param deviceId 指定设备 id
     * @param scm 客户端信息对象
     * @param autoLogout 是否为超过 maxRegClient 的自动注销
     * @param isPushWork 如果该 client 没有注册注销回调地址，是否使用 push 消息的方式进行注销回调通知
     *
     * @return /
     */
    public String notifyClientLogout(Object loginId, String deviceId, PkuseSsoClientInfo scm, boolean autoLogout, boolean isPushWork) {

        // 如果给个null值，不进行任何操作
        if(scm == null || scm.mode != PkuseSsoConsts.SSO_MODE_3) {
            return null;
        }

        // 如果此 Client 并没有注册 单点注销 回调地址
        String sloCallUrl = scm.getSloCallbackUrl();
        if(PkuseFoxUtil.isEmpty(sloCallUrl)) {
            if(isPushWork && PkuseFoxUtil.isNotEmpty(scm.getClient())) {
                PkuseSsoClientModel client = getClient(scm.getClient());
                return pushToClientByLogoutCall(client, loginId, true, getStpLogicOrGlobal().createSaLogoutParameter());
            }
            return null;
        }

        // 参数
        Map<String, Object> paramsMap = new TreeMap<>();
        paramsMap.put(paramName.client, scm.getClient());
        paramsMap.put(paramName.loginId, loginId);
        paramsMap.put(paramName.deviceId, deviceId);
        paramsMap.put(paramName.autoLogout, autoLogout);
        String signParamsStr = getSignTemplate(scm.getClient()).addSignParamsAndJoin(paramsMap);

        // 拼接
        String finalUrl = PkuseFoxUtil.joinParam(sloCallUrl, signParamsStr);

        // 发起请求
        return strategy.sendRequest.apply(finalUrl);
    }


    // ------------------- 消息推送 -------------------

    /**
     * 向指定 Client 推送消息
     * @param clientModel /
     * @param message /
     * @return /
     */
    public String pushMessage(PkuseSsoClientModel clientModel, PkuseSsoMessage message) {
        message.checkType();
        String noticeUrl = clientModel.splicingPushUrl();
        String paramsStr = getSignTemplate(clientModel.getClient()).addSignParamsAndJoin(message);
        String finalUrl = PkuseFoxUtil.joinParam(noticeUrl, paramsStr);
        return strategy.sendRequest.apply(finalUrl);
    }

    /**
     * 向指定 client 推送消息，并将返回值转为 PkuseResult
     *
     * @param clientModel /
     * @param message /
     * @return /
     */
    public PkuseResult pushMessageAsSaResult(PkuseSsoClientModel clientModel, PkuseSsoMessage message) {
        String res = pushMessage(clientModel, message);
        Map<String, Object> map = PkuseManager.getSaJsonTemplate().jsonToMap(res);
        return new PkuseResult(map);
    }

    /**
     * 向指定 Client 推送消息
     * @param client /
     * @param message /
     * @return /
     */
    public String pushMessage(String client, PkuseSsoMessage message) {
        return pushMessage(getClientNotNull(client), message);
    }

    /**
     * 向指定 client 推送消息，并将返回值转为 PkuseResult
     *
     * @param client /
     * @param message /
     * @return /
     */
    public PkuseResult pushMessageAsSaResult(String client, PkuseSsoMessage message) {
        String res = pushMessage(client, message);
        Map<String, Object> map = PkuseManager.getSaJsonTemplate().jsonToMap(res);
        return new PkuseResult(map);
    }

    /**
     * 向所有 Client 推送消息
     *
     * @param message /
     */
    public void pushToAllClient(PkuseSsoMessage message) {
        pushToAllClient(message, null);
    }

    /**
     * 向所有 Client 推送消息，并忽略掉某个 client
     *
     * @param ignoreClient 要被忽略掉的 client，填 null 代表不忽略
     * @param message /
     */
    public void pushToAllClient(PkuseSsoMessage message, String ignoreClient) {
        List<PkuseSsoClientModel> needPushClients = getNeedPushClients();
        for (PkuseSsoClientModel client : needPushClients) {
            if(PkuseFoxUtil.isNotEmpty(ignoreClient) && ignoreClient.equals(client.getClient())) {
                continue;
            }
            strategy.asyncRun.run(() -> pushMessage(client, message));
        }
    }

    /**
     * 向所有 Client 推送消息：单点注销回调
     *
     * @param loginId /
     * @param logoutParameter 注销参数
     * @param ignoreClient 要被忽略掉的 client，填 null 代表不忽略
     */
    public void pushToAllClientByLogoutCall(Object loginId, PkuseLogoutParameter logoutParameter, String ignoreClient) {
        List<PkuseSsoClientModel> npClients = getNeedPushClients();
        for (PkuseSsoClientModel client : npClients) {
            if(PkuseFoxUtil.isNotEmpty(ignoreClient) && ignoreClient.equals(client.getClient())) {
                continue;
            }
            if(client.getIsSlo()) {
                strategy.asyncRun.run(() -> {
                    pushToClientByLogoutCall(client, loginId, false, logoutParameter);
                });
            }
        }
    }

    /**
     * 向指定 Client 推送消息：单点注销回调
     *
     * @param client 应用
     * @param loginId /
     * @param autoLogout 是否为超过 maxRegClient 的自动注销
     * @param logoutParameter 注销参数
     * @return /
     */
    public String pushToClientByLogoutCall(PkuseSsoClientModel client, Object loginId, boolean autoLogout, PkuseLogoutParameter logoutParameter) {
        PkuseSsoMessage message = new PkuseSsoMessage();
        message.setType(PkuseSsoConsts.MESSAGE_LOGOUT_CALL);
        message.set(paramName.loginId, loginId);
        message.set(paramName.autoLogout, autoLogout);
        message.set(paramName.deviceId, logoutParameter.getDeviceId());
        return pushMessage(client, message);
    }


    // ------------------- Bean 获取 -------------------

    /**
     * 获取底层使用的SsoServer配置对象
     * @return /
     */
    public PkuseSsoServerConfig getServerConfig() {
        return PkuseSsoManager.getServerConfig();
    }

    /**
     * 获取底层使用的 API 签名对象
     * @param client 指定客户端标识，填 null 代表获取默认的
     * @return /
     */
    public PkuseSignTemplate getSignTemplate(String client) {
        PkuseSignConfig signConfig = PkuseSignManager.getSaSignTemplate().getSignConfigOrGlobal().copy();
        PkuseSsoClientModel clientModel = getClientNotNull(client);

        // 使用 secretKey 的优先级：client 单独配置 > SSO 模块全局配置 > sign 模块默认配置
        String secretKey = clientModel.getSecretKey();
        if (PkuseFoxUtil.isEmpty(secretKey) && PkuseFoxUtil.isNotEmpty(client)) {
            secretKey = getServerConfig().getSecretKey();
        }
        if(PkuseFoxUtil.isEmpty(secretKey)) {
            secretKey = signConfig.getSecretKey();
        }
        signConfig.setSecretKey(secretKey);

        return new PkuseSignTemplate(signConfig);
    }


    // ------------------- 返回相应key -------------------

    /**
     * 拼接key：TicketModel
     * @param ticket ticket值
     * @return key
     */
    public String splicingTicketModelSaveKey(String ticket) {
        return getStpLogicOrGlobal().getConfigOrGlobal().getTokenName() + ":ticket:" + ticket;
    }

    /**
     * 拼接key：Ticket 索引
     *
     * @param client 应用标识
     * @param id 账号id
     * @return key
     */
    public String splicingTicketIndexKey(String client, Object id) {
        if(PkuseFoxUtil.isEmpty(client) || PkuseSsoConsts.CLIENT_WILDCARD.equals(client)) {
            client = PkuseSsoConsts.CLIENT_ANON;
        }
        return getStpLogicOrGlobal().getConfigOrGlobal().getTokenName() + ":ticket-index:" + client + ":" + id;
    }

}

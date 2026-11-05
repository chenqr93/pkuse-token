package com.bdsoft.authcenter.usercenter.support;

import com.bdsoft.authcenter.usercenter.bo.UserInfo;
import com.bdsoft.pkusetoken.PkuseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户信息缓存（Redis，经 PkuseTokenDao 存储）
 *
 * <p> 登录建立会话后从用户中心取一次用户信息（id/username/nickname/role）写入本缓存， </p>
 * <p> StpInterfaceImpl 鉴权时读取，避免每次权限校验回源用户中心；TTL 与会话有效期一致。 </p>
 */
@Component
public class UserInfoCache {

    private static final Logger log = LoggerFactory.getLogger(UserInfoCache.class);

    /** 缓存键前缀 */
    private static final String KEY_PREFIX = "authcenter:userinfo:";

    /** 缓存有效期（秒）：与 sa-token 会话默认有效期一致（30 天） */
    private static final long TIMEOUT_SECONDS = 2592000L;

    private final UserCenterClient userCenterClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserInfoCache(UserCenterClient userCenterClient) {
        this.userCenterClient = userCenterClient;
    }

    /** 写入缓存（登录成功后调用） */
    public void cache(UserInfo info) {
        if (info == null || info.getId() == null) {
            return;
        }
        try {
            PkuseManager.getSaTokenDao().set(KEY_PREFIX + info.getId(),
                    objectMapper.writeValueAsString(info), TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("用户信息缓存写入失败：{}", e.getMessage());
        }
    }

    /** 读取缓存（不存在返回 null，不回源） */
    public UserInfo get(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return null;
        }
        String json = PkuseManager.getSaTokenDao().get(KEY_PREFIX + loginId);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, UserInfo.class);
        } catch (Exception e) {
            log.warn("用户信息缓存解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 读取缓存，未命中时回源用户中心（需在登录态上下文中调用）
     *
     * <p> 回源失败返回 null（表现为无角色，管理接口 403），不抛异常影响主流程。 </p>
     */
    public UserInfo getOrFetch(String loginId) {
        UserInfo cached = get(loginId);
        if (cached != null) {
            return cached;
        }
        try {
            UserInfo fetched = userCenterClient.fetchMe();
            if (fetched != null) {
                cache(fetched);
            }
            return fetched;
        } catch (Exception e) {
            log.warn("用户信息回源失败（loginId={}）：{}", loginId, e.getMessage());
            return null;
        }
    }

    /** 清除缓存（注销时调用） */
    public void evict(String loginId) {
        if (loginId != null && !loginId.isBlank()) {
            PkuseManager.getSaTokenDao().delete(KEY_PREFIX + loginId);
        }
    }
}

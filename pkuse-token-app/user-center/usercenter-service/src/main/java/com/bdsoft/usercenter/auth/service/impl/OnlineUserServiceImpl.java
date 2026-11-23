package com.bdsoft.usercenter.auth.service.impl;

import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.user.mapper.UcUserMapper;
import com.bdsoft.usercenter.auth.service.OnlineUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 在线用户 Service 实现
 *
 * <p> 通过 {@code StpUtil.searchSessionId} 搜索 Redis 中的 Account-Session（每登录账号一个， </p>
 * <p> key 形如 {@code satoken:login:session:{loginId}}），取末段即为登录 ID； </p>
 * <p> 再与 uc_user 表求交集，排除「账号已删除但会话尚未过期」的残留会话。 </p>
 */
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final Logger log = LoggerFactory.getLogger(OnlineUserServiceImpl.class);

    private final UcUserMapper userMapper;

    public OnlineUserServiceImpl(UcUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<String> onlineUserIds() {
        // Account-Session key：{tokenName}:{loginType}:session:{loginId}，末段为登录 ID
        List<String> sessionKeys = StpUtil.searchSessionId("", 0, -1, false);
        Set<String> ids = new LinkedHashSet<>();
        for (String key : sessionKeys) {
            String loginId = parseLoginId(key);
            if (loginId != null) {
                ids.add(loginId);
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        // 与用户表求交集：账号已删除/不存在的残留会话不计入在线
        Set<String> exists = new HashSet<>();
        for (UcUser user : userMapper.selectBatchIds(ids)) {
            exists.add(user.getId());
        }
        return ids.stream().filter(exists::contains).toList();
    }

    @Override
    public long onlineUserCount() {
        return onlineUserIds().size();
    }

    /** 从会话 key 末段解析登录 ID（String 主键，原样返回；空段忽略） */
    private static String parseLoginId(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return null;
        }
        String lastSegment = sessionKey.substring(sessionKey.lastIndexOf(':') + 1);
        if (lastSegment.isBlank()) {
            log.debug("忽略空登录 ID 的会话：{}", sessionKey);
            return null;
        }
        return lastSegment;
    }

}

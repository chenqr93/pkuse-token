package com.bdsoft.client.sso;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地会话存储（NoSdk 手动实现：内存存储 token → 会话信息）
 *
 * <p> 前端登录成功后拿到 token，后续请求通过 satoken 请求头携带； </p>
 * <p> 会话记录登录设备的 deviceId（来自认证中心 checkTicket 响应）， </p>
 * <p> 单点注销时（认证中心推送 logoutCall），按 loginId + deviceId 清除对应会话，避免误杀其它设备的会话。 </p>
 */
@Component
public class LocalSessionStore {

    /** token -> 会话信息 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** 创建会话，返回 token */
    public String create(Object loginId, String deviceId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(loginId, deviceId, System.currentTimeMillis()));
        return token;
    }

    /** 根据 token 查询 loginId（不存在返回 null） */
    public Object getLoginId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token);
        return session == null ? null : session.loginId;
    }

    /** 根据 token 查询登录设备 id（不存在返回 null） */
    public String getDeviceId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token);
        return session == null ? null : session.deviceId;
    }

    /** 移除指定 token 的会话 */
    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /**
     * 移除指定账号的会话（单点注销调用）
     *
     * @param loginId 账号 id
     * @param deviceId 设备 id（非空时只清除该设备的会话；为空时清除该账号全部会话）
     */
    public int removeByLoginId(Object loginId, String deviceId) {
        int count = 0;
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (session.loginId == null || !session.loginId.equals(loginId)) {
                continue;
            }
            // 指定了设备 id 时，只清除该设备的会话
            if (deviceId != null && !deviceId.isBlank() && !deviceId.equals(session.deviceId)) {
                continue;
            }
            sessions.remove(entry.getKey());
            count++;
        }
        return count;
    }

    /** 当前会话数 */
    public int size() {
        return sessions.size();
    }

    private record Session(Object loginId, String deviceId, long createTime) {
    }

}

package com.bdsoft.usercenter.authz.dto;

import java.util.List;

/**
 * 批量应用授权参数（数据传输对象：用户管理 - 批量授权）
 */
public class ClientBatchBindDTO {

    /** 授权模式：追加授权（在原有授权基础上增加） */
    public static final String MODE_APPEND = "append";

    /** 授权模式：覆盖授权（重置为本次勾选集合） */
    public static final String MODE_COVER = "cover";

    /** 目标用户 ID 集合 */
    private List<String> userIds;

    /** 本次操作的应用标识集合 */
    private List<String> clientIds;

    /**
     * 授权模式：
     * <pre>
     *   append —— 追加授权（在原有授权基础上增加，已授权的保持）
     *   cover  —— 覆盖授权（把所选用户的应用授权重置为本次勾选的集合）
     * </pre>
     */
    private String mode;

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public List<String> getClientIds() {
        return clientIds;
    }

    public void setClientIds(List<String> clientIds) {
        this.clientIds = clientIds;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "ClientBatchBindDTO{" +
                "userIds=" + userIds +
                ", clientIds=" + clientIds +
                ", mode='" + mode + '\'' +
                '}';
    }
}

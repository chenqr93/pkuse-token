package com.bdsoft.apiplatform.magic.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * magic-api 接口权限绑定（PO，表 s_api_perm）
 *
 * <p> 把 magic 接口绑定到授权中心分发的权限码（V2：接口级 RBAC）。 </p>
 * <p> 拦截器在会话校验通过后按请求路径查本表，命中则逐个校验 perms（逗号分隔，需全部满足）， </p>
 * <p> 权限数据来自授权中心客户端 starter（按 loginId 缓存）。 </p>
 */
@TableName("s_api_perm")
public class MagicApiPermPO {

    /** 主键（32 位 UUID 字符串） */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 接口请求路径（完整 URI，如 /api/v2/user/page） */
    private String path;

    /** 请求方法（GET/POST，* 表示全部） */
    private String method;

    /** 所需权限码（逗号分隔，需全部满足；来自授权中心菜单管理配置的 perms） */
    private String perms;

    /** 状态：1=启用 / 0=停用（停用后不校验） */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

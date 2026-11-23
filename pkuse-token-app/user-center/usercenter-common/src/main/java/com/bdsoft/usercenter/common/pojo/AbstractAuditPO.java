package com.bdsoft.usercenter.common.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一审计字段父类（需要审计的 PO 继承本类）
 *
 * <p> 七个审计字段：逻辑删除标记、创建人/更新人/删除人、创建时间/更新时间/删除时间。 </p>
 * <ul>
 *   <li>操作人（createBy / updateBy）为用户 ID（String，与主键同型），由 MyBatis-Plus 的
 *       MetaObjectHandler 自动填充：INSERT 填 createBy / createTime，UPDATE 填 updateBy / updateTime，
 *       操作人取当前登录用户（启动初始化等未登录场景为 null）；</li>
 *   <li>deleted 启用逻辑删除（&#64;TableLogic）：查询自动过滤已删除数据，
 *       removeById / delete 转为 UPDATE deleted=1，删除人 / 删除时间在业务删除场景手工设置。</li>
 * </ul>
 */
public abstract class AbstractAuditPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否删除：0=未删除 / 1=已删除（逻辑删除：查询自动过滤，删除转更新） */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /** 创建人（用户 ID，INSERT 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间（INSERT 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人（用户 ID，UPDATE 自动填充） */
    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    /** 更新时间（UPDATE 自动填充） */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /** 删除人（用户 ID；业务删除场景手工设置） */
    private String deleteBy;

    /** 删除时间（业务删除场景手工设置） */
    private LocalDateTime deleteTime;

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getDeleteBy() {
        return deleteBy;
    }

    public void setDeleteBy(String deleteBy) {
        this.deleteBy = deleteBy;
    }

    public LocalDateTime getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(LocalDateTime deleteTime) {
        this.deleteTime = deleteTime;
    }

    @Override
    public String toString() {
        return "AbstractAuditPO{" +
                "deleted=" + deleted +
                ", createBy='" + createBy + '\'' +
                ", createTime=" + createTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", deleteBy='" + deleteBy + '\'' +
                ", deleteTime=" + deleteTime +
                '}';
    }
}

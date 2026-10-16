package com.bdsoft.usercenter.client.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.client.dto.ClientQueryDTO;
import com.bdsoft.usercenter.client.dto.ClientSaveDTO;
import com.bdsoft.usercenter.client.po.UcClient;

import java.util.List;

/**
 * 应用（SSO Client）管理 Service
 *
 * <p> 写操作完成后会同步 SSO 客户端配置，让「应用管理」的变更即时生效。 </p>
 *
 * <p> 入参为 DTO、出参为实体，实体仅在 Service 层内部流转，不直接返回前端（对外由 ClientVO 承载）。 </p>
 */
public interface UcClientService extends IService<UcClient> {

    /** 分页查询 */
    Page<UcClient> pageQuery(ClientQueryDTO query);

    /** 查询全部应用（含禁用，按 ID 升序） */
    List<UcClient> listAll();

    /** 统计应用数（status 为 null 时统计全部） */
    long countByStatus(Integer status);

    /** 根据 ID 查询（不存在时抛业务异常） */
    UcClient getByIdOrThrow(String id);

    /** 根据应用标识查询（不存在返回 null） */
    UcClient getByClientId(String clientId);

    /** 新增应用 */
    UcClient createClient(ClientSaveDTO param);

    /** 修改应用 */
    UcClient updateClient(ClientSaveDTO param);

    /** 启用/禁用应用 */
    void changeStatus(String id, Integer status);

    /** 删除应用 */
    void deleteClient(String id);

}

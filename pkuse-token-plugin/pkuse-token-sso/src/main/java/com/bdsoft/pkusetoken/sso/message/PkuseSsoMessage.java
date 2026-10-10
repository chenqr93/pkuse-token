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
package com.bdsoft.pkusetoken.sso.message;

import com.bdsoft.pkusetoken.json.PkuseJsonType;


import com.bdsoft.pkusetoken.application.PkuseSetValueInterface;
import com.bdsoft.pkusetoken.sso.error.PkuseSsoErrorCode;
import com.bdsoft.pkusetoken.sso.exception.PkuseSsoException;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSO 消息 Model
 *
 * @author click33
 * @since 1.43.0
 */
public class PkuseSsoMessage extends LinkedHashMap<String, Object> implements PkuseSetValueInterface, PkuseJsonType, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * KEY：TYPE
     */
    public static final String MSG_TYPE = "msgType";

    public PkuseSsoMessage() {

    }

    /**
     * 构造函数
     * @param type 消息类型
     */
    public PkuseSsoMessage(String type) {
        setType(type);
    }

    /**
     * 构造函数
     * @param map 消息参数
     */
    public PkuseSsoMessage(Map<String, ?> map) {
        this.putAll(map);
    }

    /**
     * 获取消息类型
     * @return /
     */
    public String getType() {
        return getString(MSG_TYPE);
    }

    /**
     * 设置消息类型
     * @param type /
     * @return /
     */
    public PkuseSsoMessage setType(String type) {
        return set(MSG_TYPE, type);
    }

    /**
     * 校验消息类型
     */
    public void checkType() {
        if(PkuseFoxUtil.isEmpty(getString(MSG_TYPE))) {
            throw new PkuseSsoException("消息类型不可为空").setCode(PkuseSsoErrorCode.CODE_30022);
        }
    }

    // -----------

    @Override
    public Object get(String key) {
        return super.get(key);
    }

    @Override
    public PkuseSsoMessage set(String key, Object value) {
        super.put(key, value);
        return this;
    }

    @Override
    public PkuseSsoMessage delete(String key) {
        super.remove(key);
        return this;
    }

    // -----------

    /**
     * 获取一个值 （此值必须存在，否则抛出异常 ）
     * @param key 键
     * @return 参数值
     */
    public Object getValueNotNull(String key) {
        Object value = get(key);
        if(PkuseFoxUtil.isEmpty(value)) {
            throw new PkuseSsoException("缺少参数：" + key).setCode(PkuseSsoErrorCode.CODE_30024);
        }
        return value;
    }

}

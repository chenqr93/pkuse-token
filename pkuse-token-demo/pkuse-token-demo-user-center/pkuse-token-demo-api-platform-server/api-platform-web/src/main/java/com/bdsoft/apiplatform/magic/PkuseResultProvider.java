package com.bdsoft.apiplatform.magic;

import com.bdsoft.magicapi.core.config.Constants;
import com.bdsoft.magicapi.core.context.RequestEntity;
import com.bdsoft.magicapi.core.interceptor.ResultProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * magic-api 运行时接口统一响应信封：{"code":200,"msg":"ok","data":...}
 *
 * <p> magic-api 默认返回 JsonBean 五字段格式（{code,message,data,timestamp,executeTime}，成功码=1）， </p>
 * <p> 本实现把运行时脚本接口统一为平台标准三字段信封： </p>
 * <ul>
 *   <li>脚本正常返回（magic 成功码 1）→ {code:200, msg:"ok", data:...}；</li>
 *   <li>脚本异常（magic 异常码 -1）→ {code:500, msg:异常信息, data:null}；</li>
 *   <li>脚本 exit 自定义业务码 → 原样透传 {code:业务码, msg, data}（保留脚本作者的语义）。</li>
 * </ul>
 *
 * <p> 注册为 Spring Bean 后经 @ConditionalOnMissingBean 覆盖 magic-api 默认实现； </p>
 * <p> 编辑器管理接口（/magic/web/**）不经过 ResultProvider，不受本类影响。 </p>
 */
@Component
public class PkuseResultProvider implements ResultProvider {

    /** 平台标准成功码 */
    public static final int CODE_SUCCESS = 200;

    /** 平台标准失败码 */
    public static final int CODE_ERROR = 500;

    @Override
    public Object buildResult(RequestEntity requestEntity, int code, String message, Object data) {
        if (code == Constants.RESPONSE_CODE_SUCCESS) {
            return envelope(CODE_SUCCESS, "ok", data);
        }
        if (code == Constants.RESPONSE_CODE_EXCEPTION) {
            return envelope(CODE_ERROR,
                    message == null || message.isBlank() ? "系统内部出现错误" : message, null);
        }
        // 脚本 exit 自定义业务码：原样透传
        return envelope(code, message, data);
    }

    /** 构建三字段信封（LinkedHashMap 保证字段顺序：code / msg / data） */
    public static Map<String, Object> envelope(int code, String msg, Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("msg", msg);
        result.put("data", data);
        return result;
    }
}

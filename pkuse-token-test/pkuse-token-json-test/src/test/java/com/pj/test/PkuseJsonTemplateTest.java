package com.pj.test;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.exception.NotImplException;
import com.bdsoft.pkusetoken.exception.PkuseJsonConvertException;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.json.*;
import com.bdsoft.pkusetoken.session.PkuseSession;
import com.bdsoft.pkusetoken.strategy.PkuseJsonStrategy;
import com.pj.test.model.SysUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Sa-Token json 序列化模块测试
 * 
 * @author click33 
 *
 */
public class PkuseJsonTemplateTest {

	// 开始 
	@BeforeAll
    public static void beforeClass() {
    	System.out.println("\n\n------------------------ PkuseJsonTemplateTest star ...");
    }

	// 结束 
    @AfterAll
    public static void afterClass() {
    	System.out.println("\n\n------------------------ PkuseJsonTemplateTest end ... \n");
    }

    // 测试：DefaultImpl
    @Test
    public void testDefaultImpl() {
        PkuseManager.setSaJsonTemplate(new PkuseJsonTemplateDefaultImpl());
        Assertions.assertEquals(PkuseManager.getSaJsonTemplate().getClass(), PkuseJsonTemplateDefaultImpl.class);

        // test   Object -> Json
        SysUser user = new SysUser(10001, "张三", 18);
        Assertions.assertThrows(NotImplException.class, () -> PkuseManager.getSaJsonTemplate().objectToJson(user) );
        Assertions.assertThrows(NotImplException.class, () -> PkuseManager.getSaJsonTemplate().jsonToObject("xxx", SysUser.class) );
        Assertions.assertThrows(NotImplException.class, () -> PkuseManager.getSaJsonTemplate().jsonToObject("xxx") );
        Assertions.assertThrows(NotImplException.class, () -> PkuseManager.getSaJsonTemplate().jsonToMap("xxx") );
    }

    // 测试：Jackson
    @Test
    public void testJackson() {
        PkuseJsonStrategy.instance.resetState();
        PkuseManager.setSaJsonTemplate(new PkuseJsonTemplateForJackson());
        Assertions.assertEquals(PkuseManager.getSaJsonTemplate().getClass(), PkuseJsonTemplateForJackson.class);
        Assertions.assertTrue(PkuseJsonStrategy.instance.isInit());

        // test   Object -> Json
        SysUser user = new SysUser(10001, "张三", 18);
        String objectJson = PkuseManager.getSaJsonTemplate().objectToJson(user);
        Assertions.assertEquals("{\"@class\":\"com.pj.test.model.SysUser\",\"id\":10001,\"name\":\"张三\",\"age\":18,\"role\":null}", objectJson);

        // test   Json -> Object
        SysUser user2 = PkuseManager.getSaJsonTemplate().jsonToObject(objectJson, SysUser.class);
        Assertions.assertEquals(user2.toString(), user.toString());

        SysUser user3 = (SysUser)PkuseManager.getSaJsonTemplate().jsonToObject(objectJson);
        Assertions.assertEquals(user3.toString(), user.toString());

        // more
        testNull();
        testMap();
    }

    // 测试：Jackson 允许 Session 中常见的 JDK 值类型（Date、金额、java.time 等）
    @Test
    public void testJacksonAllowCommonTypesInSession() {
        PkuseJsonStrategy.instance.resetState();
        PkuseManager.setSaJsonTemplate(new PkuseJsonTemplateForJackson());
        Date time = new Date(1234567890000L);
        BigDecimal amount = new BigDecimal("99.50");
        LocalDateTime loginTime = LocalDateTime.of(2026, 8, 15, 12, 30, 0);
        PkuseSession session = new PkuseSession("test-session");
        session.set("time", time);
        session.set("amount", amount);
        session.set("loginTime", loginTime);
        String json = PkuseManager.getSaJsonTemplate().objectToJson(session);
        PkuseSession session2 = PkuseManager.getSaJsonTemplate().jsonToObject(json, PkuseSession.class);
        Assertions.assertEquals(time, session2.get("time"));
        Assertions.assertEquals(amount, session2.get("amount"));
        Assertions.assertEquals(loginTime, session2.get("loginTime"));
    }

    // 测试：Jackson 允许 Session Map 中的基本类型包装类（如 Long 过期时间戳）
    @Test
    public void testJacksonAllowWrapperTypesInSessionMap() {
        PkuseJsonStrategy.instance.resetState();
        PkuseManager.setSaJsonTemplate(new PkuseJsonTemplateForJackson());
        java.util.LinkedHashMap<String, Long> tokenIndexMap = new java.util.LinkedHashMap<>();
        tokenIndexMap.put("access-token-value", 1786860583046L);
        PkuseSession session = new PkuseSession("oauth2-raw-session");
        session.set("__HD_ACCESS_TOKEN_MAP", tokenIndexMap);
        String json = PkuseManager.getSaJsonTemplate().objectToJson(session);
        PkuseSession session2 = PkuseManager.getSaJsonTemplate().jsonToObject(json, PkuseSession.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Long> map2 = (java.util.Map<String, Long>) session2.get("__HD_ACCESS_TOKEN_MAP");
        Assertions.assertEquals(1786860583046L, map2.get("access-token-value"));
    }

    // 测试：Jackson 白名单拦截未授权 @class
    @Test
    public void testJacksonBlockUnknownAllowType() {
        PkuseJsonStrategy.instance.resetState();
        PkuseManager.setSaJsonTemplate(new PkuseJsonTemplateForJackson());
        String evilJson = "{\"@class\":\"java.lang.ProcessBuilder\",\"command\":[\"calc.exe\"]}";
        PkuseJsonConvertException ex = Assertions.assertThrows(PkuseJsonConvertException.class, () ->
                PkuseManager.getSaJsonTemplate().jsonToObject(evilJson));
        Assertions.assertEquals(
                "无法反序列化的类型：java.lang.ProcessBuilder，请先将其注册到 JSON 全局类型白名单",
                ex.getMessage());
    }

    // 测试：Jackson 遇到 classpath 不存在的类型时，保留原始异常信息
    @Test
    public void testJacksonClassNotFoundKeepsOriginalMessage() {
        PkuseJsonStrategy.instance.resetState();
        PkuseManager.setSaJsonTemplate(new PkuseJsonTemplateForJackson());
        String json = "{\"@class\":\"com.bdsoft.pkusetoken.sso.model.PkuseSsoClientInfo\",\"mode\":3}";
        PkuseJsonConvertException ex = Assertions.assertThrows(PkuseJsonConvertException.class, () ->
                PkuseManager.getSaJsonTemplate().jsonToObject(json));
        Assertions.assertTrue(ex.getMessage().contains("no such class found"));
        Assertions.assertFalse(ex.getMessage().contains("JSON 全局类型白名单"));
    }

    // 测试：Jackson 初始化后不可再 register
    @Test
    public void testJacksonStrategyInit() {
        PkuseJsonStrategy.instance.resetState();
        PkuseManager.setSaJsonTemplate(new PkuseJsonTemplateForJackson());
        Assertions.assertThrows(PkuseTokenException.class, () ->
                PkuseJsonStrategy.instance.registerAllowType(String.class));
    }

    // 测试 Map 的转换
    private void testMap() {

        // test   Map -> Json
        Map<String, Object> map = new HashMap<>();
        map.put("id", 10001);
        map.put("name", "张三");
        map.put("age", 18);
        String mapJson = PkuseManager.getSaJsonTemplate().objectToJson(map);
        Assertions.assertEquals("{\"name\":\"张三\",\"id\":10001,\"age\":18}", mapJson);

        // test   Json -> Map
        Map<String, Object> map2 = PkuseManager.getSaJsonTemplate().jsonToMap(mapJson);
        Assertions.assertEquals(map2.toString(), map.toString());

    }

    // 测试 Null 值
    private void testNull() {
        Assertions.assertNull(PkuseManager.getSaJsonTemplate().objectToJson(null));
        Assertions.assertNull(PkuseManager.getSaJsonTemplate().jsonToObject(null, SysUser.class));
        Assertions.assertNull(PkuseManager.getSaJsonTemplate().jsonToObject(null));
        Assertions.assertNull(PkuseManager.getSaJsonTemplate().jsonToMap(null));
    }

}


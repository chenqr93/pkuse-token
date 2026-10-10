package com.bdsoft.pkusetoken.aop;

import com.bdsoft.pkusetoken.annotation.handler.PkuseAnnotationHandlerInterface;
import com.bdsoft.pkusetoken.strategy.PkuseAnnotationStrategy;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token AOP 环绕切入 Bean 注册
 * <p>
 *    参考资料：<br>
 *    https://www.jb51.net/program/297714rev.htm    <br>
 *    https://www.bilibili.com/video/BV1WZ421W7Qx   <br>
 *    https://blog.csdn.net/Tomwildboar/article/details/139199801   <br>
 * </p>
 *
 * @author click33
 * @since 2024/8/3
 */
@Configuration
public class PkuseAopPointcutAdvisorBeanRegister {

    /**
     * Advisor 静态全局引用
     */
    public static PkuseAroundAnnotationPointcutAdvisor saAroundAnnoAdvisor;

    @Bean
    public PkuseAroundAnnotationPointcutAdvisor saAroundAnnotationHandlePointcutAdvisor (List<PkuseAnnotationHandlerInterface<?>> handlerList) {
        PkuseAroundAnnotationPointcutAdvisor advisor = new PkuseAroundAnnotationPointcutAdvisor();

        // 定义切入规则
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        String expression = calcExpression(handlerList);
        pointcut.setExpression(expression);
        advisor.setPointcut(pointcut);

        // 定义执行的方法 
        advisor.setAdvice(new PkuseAroundAnnotationMethodInterceptor());

        // 保存全局引用
        PkuseAopPointcutAdvisorBeanRegister.saAroundAnnoAdvisor = advisor;
        return advisor;
    }

    /**
     * 计算切入表达式
     * @param appendHandlerList 追加的 PkuseAnnotationAbstractHandler 处理器
     * @return /
     */
    public static String calcExpression(List<PkuseAnnotationHandlerInterface<?>> appendHandlerList) {

        // 框架内置的
        List<Class<?>> list = new ArrayList<>(PkuseAnnotationStrategy.instance.annotationHandlerMap.keySet());

        // 额外追加的
        if(appendHandlerList != null) {
            for (PkuseAnnotationHandlerInterface<?> handler : appendHandlerList) {
                Class<?> cls = handler.getHandlerAnnotationClass();
                if(!list.contains(cls)) {
                    list.add(handler.getHandlerAnnotationClass());
                }
            }
        }

        // 计算
        return calcClassListExpression(list);
    }

    /**
     * 计算 class 列表的切入表达式，
     * 最终样例形如：
     <pre>
         public static final String POINTCUT_SIGN =
         "@within(com.bdsoft.pkusetoken.annotation.PkuseCheckLogin) || @annotation(com.bdsoft.pkusetoken.annotation.PkuseCheckLogin) || "
         + "@within(com.bdsoft.pkusetoken.annotation.PkuseCheckRole) || @annotation(com.bdsoft.pkusetoken.annotation.PkuseCheckRole) || "
         + "@within(com.bdsoft.pkusetoken.annotation.PkuseCheckPermission) || @annotation(com.bdsoft.pkusetoken.annotation.PkuseCheckPermission)";
     </pre>
     * @param list /
     * @return /
     */
    public static String calcClassListExpression(List<Class<?>> list) {
        String pointcutExpression = "";
        for (Class<?> cls : list) {
            if(!pointcutExpression.isEmpty()) {
                pointcutExpression += " || ";
            }
            pointcutExpression += "@within(" + cls.getName() + ") || @annotation(" + cls.getName() + ")";
        }
        return pointcutExpression;
    }


}
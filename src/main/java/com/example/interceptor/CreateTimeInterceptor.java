package com.example.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * 拦截 {@link Executor#update}，自动填充实体的 {@code createTime} 字段（为空时才填）。
 *
 * 教学实现取舍与局限：
 * - 通过反射按固定字段名 "createTime" 定位；遇 Map 参数（如 {@code @Param("list")}）时
 *   按包名前缀 "com.example.entity." 识别实体 —— 仅适配本项目结构。
 *   更稳的做法：依据 {@link MappedStatement#getParameterType()} 或自定义注解定位实体。
 * - 不区分 insert/update：只要 createTime 为空就填充。update 场景若不希望改写，
 *   可通过 statement.getId() 判断是否 insert 语句后再填。
 * - 实体需提供可写（setAccessible）的 createTime 字段；缺失时静默跳过并记 DEBUG。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class CreateTimeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(CreateTimeInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object parameter = invocation.getArgs()[1];
        fillCreateTime(parameter);
        return invocation.proceed();
    }

    private void fillCreateTime(Object parameter) {
        Object target = resolveEntity(parameter);
        if (target == null) {
            return;
        }
        try {
            Field field = target.getClass().getDeclaredField("createTime");
            field.setAccessible(true);
            if (field.get(target) == null) {
                field.set(target, new Date());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Skip createTime fill for {}", target.getClass().getSimpleName());
        }
    }

    private Object resolveEntity(Object parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof Map) {
            for (Object value : ((Map<?, ?>) parameter).values()) {
                if (value != null && value.getClass().getName().startsWith("com.example.entity.")) {
                    return value;
                }
            }
            return null;
        }
        return parameter;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}

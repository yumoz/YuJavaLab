package com.example.interceptor;

import com.example.entity.User;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateTimeInterceptorTest {

    @Test
    public void intercept_shouldFillCreateTimeOnEntity() throws Throwable {
        User user = new User("a", "b", "c@test.com");
        assertNull(user.getCreateTime());

        Executor executor = mock(Executor.class);
        when(executor.update(null, user)).thenReturn(1);

        CreateTimeInterceptor interceptor = new CreateTimeInterceptor();
        // MappedStatement 是 final 类，subclass mock maker 无法 mock；
        // 拦截器只读取 args[1]（实体），args[0] 用 null 占位即可，不影响测试意图
        Object result = interceptor.intercept(new Invocation(
                executor, Executor.class.getMethod("update", MappedStatement.class, Object.class),
                new Object[]{null, user}));

        assertEquals(1, result);
        assertNotNull(user.getCreateTime());
        verify(executor).update(null, user);
    }
}

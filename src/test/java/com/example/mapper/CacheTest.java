package com.example.mapper;

import com.example.entity.User;
import com.example.service.UserService;
import com.example.service.impl.UserServiceImpl;
import com.example.util.DatabaseInit;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * 二级缓存命中/失效演示测试。
 *
 * UserMapper.xml 配置了 {@code <cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>}：
 * - readOnly 模式下二级缓存缓存对象引用本身，因此同一查询两次（跨 SqlSession）应返回同一实例；
 * - 任何对该 Mapper namespace 的 insert/update/delete 默认 flushCache=true，会清空二级缓存。
 *
 * 每个用例在 {@link Before} 重建数据库，保证从干净的种子数据开始（缓存为进程内状态，
 * DB 重建不影响已缓存的 id=1 对象引用，故本测试对引用相等性的断言不受影响）。
 */
public class CacheTest {

    private final UserService userService = new UserServiceImpl();

    @Before
    public void resetDb() {
        DatabaseInit.init();
    }

    @Test
    public void secondQuery_shouldHitSecondLevelCache() {
        // 每次调用各开一个新 SqlSession；第一次查询结束后缓存写入，第二次命中二级缓存
        User first = userService.selectById(1);
        User second = userService.selectById(1);
        // readOnly 缓存返回同一对象引用
        assertSame("两次查询应命中二级缓存并返回同一实例", first, second);
    }

    @Test
    public void insert_shouldClearSecondLevelCache() {
        User first = userService.selectById(1);
        // insert 触发该 namespace 缓存清空
        User tmp = new User("cache_evict_" + UUID.randomUUID().toString().substring(0, 8), "p", "e@t.com");
        userService.insertUser(tmp);
        User second = userService.selectById(1);
        // 缓存清空后重新查询，得到新实例
        assertNotSame("insert 后二级缓存应被清空，重新查询得到新实例", first, second);
    }
}
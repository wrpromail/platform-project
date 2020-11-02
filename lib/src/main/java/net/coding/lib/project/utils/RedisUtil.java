package net.coding.lib.project.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Component
public class RedisUtil {

    @Autowired
    private JedisPool jedisPool;

    private final String prefix = "platformProject";

    public boolean set(String key, Object value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            if (Objects.isNull(value)) {
                return false;
            }
            String realKey = prefix + key;
            jedis.set(realKey, value.toString());
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    public boolean set(String key, Object value, int expireTime) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            if (Objects.isNull(value)) {
                return false;
            }
            String realKey = prefix + key;
            jedis.set(realKey, value.toString());
            jedis.expire(realKey, expireTime);
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    public String get(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix + key;
            return jedis.get(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    public Long incr(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix + key;
            return jedis.incr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    public <T> Long decr(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix + key;
            return jedis.decr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    public boolean exists(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix + key;
            return jedis.exists(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    private void returnToPool(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}

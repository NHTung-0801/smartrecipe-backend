package com.smartrecipe.smartrecipe_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(jsonSerializer)
                .disableCachingNullValues();

        // Cấu hình riêng cho Master Data - TTL dài hơn (ít thay đổi)
        RedisCacheConfiguration masterDataConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(jsonSerializer)
                .disableCachingNullValues();

        // Cấu hình riêng cho search - TTL ngắn hơn (kết quả thay đổi thường xuyên)
        RedisCacheConfiguration searchConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeValuesWith(jsonSerializer)
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // Aisles
                .withCacheConfiguration("aisles", masterDataConfig)
                .withCacheConfiguration("aisle", masterDataConfig)
                // Tags
                .withCacheConfiguration("tags", masterDataConfig)
                .withCacheConfiguration("tag", masterDataConfig)
                // Ingredients
                .withCacheConfiguration("ingredients_search", searchConfig)
                .withCacheConfiguration("ingredients_by_aisle", masterDataConfig)
                .withCacheConfiguration("ingredient", masterDataConfig)
                // Unit Conversions
                .withCacheConfiguration("unit_conversions", masterDataConfig)
                .withCacheConfiguration("unit_conversions_by_ingredient", masterDataConfig)
                .withCacheConfiguration("unit_conversions_generic", masterDataConfig)
                .build();
    }
}
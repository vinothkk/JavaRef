# Redis Configuration - Fixed Version
spring.cache.type=redis

# Use individual node endpoints for cluster mode
spring.redis.cluster.nodes=dpe0113-dev-redisclusterc360-001.dpe0113-dev-redisclusterc360.emq5bj.use1.cache.amazonaws.com:6379,dpe0113-dev-redisclusterc360-002.dpe0113-dev-redisclusterc360.emq5bj.use1.cache.amazonaws.com:6379

# SSL Configuration
spring.data.redis.ssl=true
spring.data.redis.timeout=10000ms

# Connection Pool Configuration (fixed duplicate min-idle)
spring.data.redis.jedis.pool.max-active=20
spring.data.redis.jedis.pool.max-wait=3000ms
spring.data.redis.jedis.pool.max-idle=10
spring.data.redis.jedis.pool.min-idle=2

# Lettuce pool configuration (recommended over Jedis for cluster)
spring.data.redis.lettuce.pool.max-active=20
spring.data.redis.lettuce.pool.max-wait=3000ms
spring.data.redis.lettuce.pool.max-idle=10
spring.data.redis.lettuce.pool.min-idle=2


@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.redis.cluster.nodes}")
    private String clusterNodes;

    @Value("${spring.data.redis.ssl:false}")
    private boolean enableSsl;

    @Bean
    public KeyGenerator customKeyGenerator() {
        return new CustomKeyGenerator();
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        List<String> nodes = Arrays.asList(clusterNodes.split(","));
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(nodes);
        
        // Configure SSL if enabled
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = 
            LettuceClientConfiguration.builder();
        
        if (enableSsl) {
            clientConfigBuilder.useSsl().disablePeerVerification();
        }
        
        LettuceClientConfiguration clientConfig = clientConfigBuilder
            .commandTimeout(Duration.ofMillis(10000))
            .build();
        
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}

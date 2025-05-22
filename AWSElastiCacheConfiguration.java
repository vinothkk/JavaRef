// AWSElastiCacheConfiguration
@Configuration
@EnableCaching
public class AwsRedisConfig {
    
    @Value("${spring.redis.host}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Bean
    public KeyGenerator customKeyGenerator() {
        return new CustomKeyGenerator();
    }
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        
        // For AWS ElastiCache, you typically don't need password if using VPC security groups
        // If you do need authentication:
        // redisConfig.setPassword(RedisPassword.of("your-auth-token"));
        
        return new LettuceConnectionFactory(redisConfig);
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
    }
}

// Fallback Service - Normal Flow When Redis is Down
@Service
public class ProductServicesServiceImplWithFallback {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductServicesServiceImplWithFallback.class);
    
    @Lazy
    @Autowired
    private ProductServicesServiceImplWithFallback self;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Method with cache and fallback
    public List<ParentDataRecordDTO> getProductServicesData(GlobalFilterDTO globalFilterDTO) {
        try {
            // Try to use cache first
            return self.getProductServicesDataCached(globalFilterDTO);
        } catch (Exception e) {
            logger.warn("Redis cache failed, falling back to direct database call: {}", e.getMessage());
            // Fallback to normal flow without cache
            return fetchProductServicesFromDatabase(globalFilterDTO);
        }
    }
    
    // Cached method
    @Cacheable(value = "productServicesData", keyGenerator = "customKeyGenerator")
    public List<ParentDataRecordDTO> getProductServicesDataCached(GlobalFilterDTO globalFilterDTO) {
        logger.info("Cache MISS - Fetching from database");
        return fetchProductServicesFromDatabase(globalFilterDTO);
    }
    
    // Your original API method
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        
        logger.info("ProductServicesServiceImpl: getProductServicesOptimization()");
        
        // This will try cache first, then fallback to normal flow
        List<ParentDataRecordDTO> data = getProductServicesData(globalFilterDTO);
        
        ResponseData<List<ParentDataRecordDTO>> responseData = new ResponseData<>();
        responseData.setResponseCode("200");
        responseData.setResponseMessage("SUCCESS");
        responseData.setResponseBody(data);
        
        return ResponseEntity.ok(responseData);
    }
    
    // Your actual data fetching logic (normal flow)
    private List<ParentDataRecordDTO> fetchProductServicesFromDatabase(GlobalFilterDTO globalFilterDTO) {
        logger.info("Fetching data from database/service");
        // Your existing implementation
        return new ArrayList<>(); // Replace with your actual implementation
    }
    
    // Method to check Redis health
    public boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            logger.warn("Redis is not available: {}", e.getMessage());
            return false;
        }
    }
}

// Alternative: Configuration-based fallback
@Configuration
public class CacheConfiguration {
    
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            // Test Redis connection
            RedisConnection connection = redisConnectionFactory.getConnection();
            connection.ping();
            connection.close();
            
            // Redis is available, use Redis cache
            RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
            
            return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
                
        } catch (Exception e) {
            logger.warn("Redis not available, using in-memory cache: {}", e.getMessage());
            // Fallback to simple in-memory cache
            return new ConcurrentMapCacheManager();
        }
    }
}

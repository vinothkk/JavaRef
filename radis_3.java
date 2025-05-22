// SOLUTION 1: Simple Redis Configuration (Recommended)
// No custom serializers, just cache the data, not ResponseEntity
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public KeyGenerator customKeyGenerator() {
        return new CustomKeyGenerator();
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Use default GenericJackson2JsonRedisSerializer - no custom ObjectMapper needed
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
    }
}

// SOLUTION 2: Minimal Changes to Your Existing Service
// Just change what you cache - cache the data, not the ResponseEntity
@Service
public class ProductServicesServiceImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductServicesServiceImpl.class);
    
    // Cache only the data part - this will work with default serializer
    @Cacheable(value = "productServicesData", keyGenerator = "customKeyGenerator")
    public List<ParentDataRecordDTO> getCachedProductServicesData(GlobalFilterDTO globalFilterDTO) {
        logger.info("ProductServicesServiceImpl: fetching product services data - CACHE MISS");
        
        // Move your existing data fetching logic here
        return fetchProductServicesFromDatabase(globalFilterDTO);
    }
    
    // Your original method with minimal changes
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        
        logger.info("ProductServicesServiceImpl: getProductServicesOptimization()");
        
        // Get cached data - only this line changes
        List<ParentDataRecordDTO> data = getCachedProductServicesData(globalFilterDTO);
        
        // Rest of your code stays exactly the same
        ResponseData<List<ParentDataRecordDTO>> responseData = new ResponseData<>();
        responseData.setResponseCode("200");
        responseData.setResponseMessage("SUCCESS");
        responseData.setResponseBody(data);
        
        return ResponseEntity.ok(responseData);
    }
    
    // Your existing data fetching logic (no changes needed)
    private List<ParentDataRecordDTO> fetchProductServicesFromDatabase(GlobalFilterDTO globalFilterDTO) {
        // Your existing implementation
        // Database calls, repository calls, etc.
        return new ArrayList<>(); // Replace with your actual implementation
    }
}

// SOLUTION 3: Even Simpler - Use Separate Cache Service (One-time setup)
// Create this service once, then use it everywhere
@Service 
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    // Generic cacheable method that works with any data type
    @Cacheable(value = "genericCache", keyGenerator = "customKeyGenerator")
    public <T> T getCachedData(String cacheKey, Supplier<T> dataSupplier) {
        logger.info("Cache MISS for key: {}", cacheKey);
        return dataSupplier.get();
    }
    
    // Or specific method for your use case
    @Cacheable(value = "productServicesData", keyGenerator = "customKeyGenerator") 
    public List<ParentDataRecordDTO> getProductServicesData(
            GlobalFilterDTO globalFilterDTO, 
            Supplier<List<ParentDataRecordDTO>> dataSupplier) {
        logger.info("Cache MISS for product services data");
        return dataSupplier.get();
    }
}

// How to use the cache service in your existing classes
@Service
public class ProductServicesServiceImplWithCacheService {
    
    @Autowired
    private CacheService cacheService;
    
    // Your original method with just 2 lines changed
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        
        logger.info("ProductServicesServiceImpl: getProductServicesOptimization()");
        
        // Use cache service - only these 2 lines change
        List<ParentDataRecordDTO> data = cacheService.getProductServicesData(
            globalFilterDTO, 
            () -> fetchProductServicesFromDatabase(globalFilterDTO)
        );
        
        // Rest of your code stays exactly the same
        ResponseData<List<ParentDataRecordDTO>> responseData = new ResponseData<>();
        responseData.setResponseCode("200");
        responseData.setResponseMessage("SUCCESS");
        responseData.setResponseBody(data);
        
        return ResponseEntity.ok(responseData);
    }
    
    private List<ParentDataRecordDTO> fetchProductServicesFromDatabase(GlobalFilterDTO globalFilterDTO) {
        // Your existing implementation
        return new ArrayList<>();
    }
}

// SOLUTION 4: Fix Self-Invocation with @Lazy Self-Injection (Spring 4.3+)
@Service
public class ProductServicesServiceImplWithSelfInjection {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductServicesServiceImplWithSelfInjection.class);
    
    // Self-inject the proxy to avoid self-invocation issue
    @Lazy
    @Autowired
    private ProductServicesServiceImplWithSelfInjection self;
    
    // This method will be cached
    @Cacheable(value = "productServicesData", keyGenerator = "customKeyGenerator")
    public List<ParentDataRecordDTO> getProductServicesDataCached(GlobalFilterDTO globalFilterDTO) {
        logger.info("ProductServicesServiceImpl: fetching product services data - CACHE MISS");
        return fetchProductServicesFromDatabase(globalFilterDTO);
    }
    
    // Your original method with minimal change
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        
        logger.info("ProductServicesServiceImpl: getProductServicesOptimization()");
        
        // Call via self-injected proxy - only this line changes
        List<ParentDataRecordDTO> data = self.getProductServicesDataCached(globalFilterDTO);
        
        // Rest stays the same
        ResponseData<List<ParentDataRecordDTO>> responseData = new ResponseData<>();
        responseData.setResponseCode("200");
        responseData.setResponseMessage("SUCCESS");
        responseData.setResponseBody(data);
        
        return ResponseEntity.ok(responseData);
    }
    
    private List<ParentDataRecordDTO> fetchProductServicesFromDatabase(GlobalFilterDTO globalFilterDTO) {
        // Your existing implementation
        return new ArrayList<>();
    }
}

// SOLUTION 5: Application Properties Configuration
// Add these to your application.properties or application.yml

/*
# Redis Configuration (application.properties)
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=60000
spring.redis.jedis.pool.max-active=8
spring.redis.jedis.pool.max-wait=-1
spring.redis.jedis.pool.max-idle=8
spring.redis.jedis.pool.min-idle=0

# Enable cache logging
logging.level.org.springframework.cache=DEBUG
*/

// Or in application.yml
/*
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 60000
    jedis:
      pool:
        max-active: 8
        max-wait: -1
        max-idle: 8
        min-idle: 0

logging:
  level:
    org.springframework.cache: DEBUG
*/

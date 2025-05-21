Second
=============
// SOLUTION 1: Extract caching logic to a separate service
// =====================================================

// Create a dedicated caching service
@Service
public class ProductDataCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductDataCacheService.class);
    
    @Autowired
    private ProductDataRepository repository; // Your actual data source
    
    @Cacheable(value = "productServicesData", keyGenerator = "customKeyGenerator")
    public List<ParentDataRecordDTO> getProductServicesData(GlobalFilterDTO globalFilterDTO) {
        logger.info("Cache MISS: Fetching product services data from source");
        // Your logic to fetch data
        return repository.fetchProductData(globalFilterDTO);
    }
}

// Then in your main service
@Service
public class ProductServicesServiceImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductServicesServiceImpl.class);
    
    @Autowired
    private ProductDataCacheService cacheService;
    
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        
        logger.info("ProductServicesServiceImpl: getProductServicesOptimization()");
        
        // Get the data from the dedicated cache service
        List<ParentDataRecordDTO> data = cacheService.getProductServicesData(globalFilterDTO);
        
        ResponseData<List<ParentDataRecordDTO>> responseData = new ResponseData<>();
        responseData.setResponseCode("200");
        responseData.setResponseMessage("SUCCESS");
        responseData.setResponseBody(data);
        
        return ResponseEntity.ok(responseData);
    }
}

// SOLUTION 2: Use AspectJ mode instead of proxy mode
// ===================================================

// In your Spring Boot application main class
@SpringBootApplication
@EnableCaching(mode = AdviceMode.ASPECTJ) // Enable AspectJ mode
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}

// SOLUTION 3: Use CacheManager directly
// =====================================

@Service
public class ProductServicesServiceImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductServicesServiceImpl.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private ProductDataRepository repository; // Your actual data source
    
    public List<ParentDataRecordDTO> getProductServicesData(GlobalFilterDTO globalFilterDTO) {
        // Generate cache key using same logic as your custom key generator
        String cacheKey = generateCacheKey(globalFilterDTO);
        
        // Try to get from cache
        Cache cache = cacheManager.getCache("productServicesData");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                logger.info("Cache HIT for key: {}", cacheKey);
                return (List<ParentDataRecordDTO>) wrapper.get();
            }
        }
        
        // Cache miss, fetch data
        logger.info("Cache MISS for key: {}", cacheKey);
        List<ParentDataRecordDTO> data = repository.fetchProductData(globalFilterDTO);
        
        // Store in cache
        if (cache != null) {
            cache.put(cacheKey, data);
        }
        
        return data;
    }
    
    private String generateCacheKey(GlobalFilterDTO filter) {
        return new StringBuilder()
            .append("data")
            .append(":")
            .append(StringUtils.collectionToDelimitedString(filter.getCountries(), "-"))
            .append(":")
            .append(StringUtils.collectionToDelimitedString(filter.getSegments(), "-"))
            .append(":")
            .append(StringUtils.collectionToDelimitedString(filter.getRegions(), "-"))
            .toString();
    }
    
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        
        List<ParentDataRecordDTO> data = getProductServicesData(globalFilterDTO);
        
        ResponseData<List<ParentDataRecordDTO>> responseData = new ResponseData<>();
        responseData.setResponseCode("200");
        responseData.setResponseMessage("SUCCESS");
        responseData.setResponseBody(data);
        
        return ResponseEntity.ok(responseData);
    }
}

// SOLUTION 4: Debug & Verify Redis Configuration
// ==============================================

@Configuration
@EnableCaching
public class RedisConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Bean
    public KeyGenerator customKeyGenerator() {
        return new CustomKeyGenerator();
    }
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("Configuring Redis connection to {}:{}", redisHost, redisPort);
        
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        
        return new LettuceConnectionFactory(redisConfig);
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        // Log when cache manager is initialized
        logger.info("Redis cache manager initialized with TTL: {} minutes", 30);
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
    }
    
    // Test that Redis is actually working
    @PostConstruct
    public void testRedisConnection() {
        try {
            RedisConnection connection = redisConnectionFactory().getConnection();
            String pong = new String(connection.ping());
            logger.info("Redis connection test - response: {}", pong);
            connection.close();
        } catch (Exception e) {
            logger.error("Failed to connect to Redis: {}", e.getMessage(), e);
        }
    }
}

// SOLUTION 5: Verify with debug logging
// =====================================

// Add this to your application.properties or application.yml
/*
# Enable caching debug logs
logging.level.org.springframework.cache=TRACE
logging.level.org.springframework.data.redis=DEBUG
*/
==========================================================================
Fisrt issue solution for the above.
========================================
    // Option 1: Cache the data object instead of ResponseEntity
@Service
public class ProductServicesServiceImpl {

    // Instead of caching the ResponseEntity, cache just the data
    @Cacheable(value = "productServicesData", keyGenerator = "customKeyGenerator")
    public List<ParentDataRecordDTO> getProductServicesData(GlobalFilterDTO globalFilterDTO) {
        logger.info("ProductServicesServiceImpl: fetching product services data");
        // Your logic to fetch the data
        List<ParentDataRecordDTO> data = // fetch data
        return data;
    }

    // Public API method that uses the cached data
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        
        // Get the data from cached method
        List<ParentDataRecordDTO> data = getProductServicesData(globalFilterDTO);
        
        // Create the ResponseEntity on the fly (this part won't be cached)
        ResponseData<List<ParentDataRecordDTO>> responseData = new ResponseData<>();
        responseData.setResponseCode("200");
        responseData.setResponseMessage("SUCCESS");
        responseData.setResponseBody(data);
        
        return ResponseEntity.ok(responseData);
    }
}

// Option 2: Use a custom serializer/deserializer for ResponseEntity
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public KeyGenerator customKeyGenerator() {
        return new CustomKeyGenerator();
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create a custom ObjectMapper with appropriate modules
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register a module to handle ResponseEntity serialization
        SimpleModule module = new SimpleModule();
        module.addSerializer(ResponseEntity.class, new ResponseEntitySerializer());
        module.addDeserializer(ResponseEntity.class, new ResponseEntityDeserializer());
        objectMapper.registerModule(module);
        
        // Create Jackson2JsonRedisSerializer with custom ObjectMapper
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(objectMapper);
        
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(serializer));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
    }
}

// Custom serializer for ResponseEntity
class ResponseEntitySerializer extends JsonSerializer<ResponseEntity<?>> {
    @Override
    public void serialize(ResponseEntity<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("status", value.getStatusCodeValue());
        gen.writeObjectField("headers", value.getHeaders());
        gen.writeObjectField("body", value.getBody());
        gen.writeEndObject();
    }
}

// Custom deserializer for ResponseEntity
class ResponseEntityDeserializer extends JsonDeserializer<ResponseEntity<?>> {
    @Override
    public ResponseEntity<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        int status = node.get("status").asInt();
        HttpHeaders headers = ctxt.readValue(node.get("headers").traverse(p.getCodec()), HttpHeaders.class);
        Object body = ctxt.readValue(node.get("body").traverse(p.getCodec()), Object.class);
        
        return ResponseEntity.status(status).headers(headers).body(body);
    }
}
============================================================================================
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

# Redis Configuration
spring.cache.type=redis
spring.redis.host=${REDIS_HOST}
spring.redis.port=6379
spring.redis.ssl=false
spring.redis.timeout=2000

# Connection pool settings
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=2
spring.redis.lettuce.pool.max-wait=-1ms

# TTL settings (optional)
spring.cache.redis.time-to-live=3600000

Create Redis configuration class:

@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60))
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

Implement caching in your service:
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(String id) {
        // Method will be executed only if the result is not in the cache
        return userRepository.findById(id);
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        // Method always executes and its result is stored in the cache
        return userRepository.save(user);
    }
    
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(String id) {
        // Removes the cache entry
        userRepository.deleteById(id);
    }
}

if my payload is an object means then how to use the key in radis like {country:[], segment:[], regin:[]}

1. Key Generation Strategies for Complex Objects
Option 1: Custom Key Generator
Create a custom key generator to handle your complex object structure:

@Component
public class CustomKeyGenerator implements KeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length == 0) {
            return SimpleKey.EMPTY;
        }
        
        // Assuming the first parameter is your filter object
        if (params[0] instanceof FilterCriteria) {
            FilterCriteria filter = (FilterCriteria) params[0];
            
            // Create a key based on the filter's properties
            return new StringBuilder()
                .append("data")
                .append(":")
                .append(StringUtils.collectionToDelimitedString(filter.getCountries(), "-"))
                .append(":")
                .append(StringUtils.collectionToDelimitedString(filter.getSegments(), "-"))
                .append(":")
                .append(StringUtils.collectionToDelimitedString(filter.getRegions(), "-"))
                .toString();
        }
        
        // Fallback to default key generation
        return new SimpleKey(params);
    }
}

2. Creating the Filter Criteria Class
Define a proper class to represent your filter criteria:

public class FilterCriteria {
    private List<String> countries;
    private List<String> segments;
    private List<String> regions;
    
    // Constructors, getters, setters
    
    // Ensure proper equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterCriteria that = (FilterCriteria) o;
        return Objects.equals(countries, that.countries) &&
               Objects.equals(segments, that.segments) &&
               Objects.equals(regions, that.regions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(countries, segments, regions);
    }
}

3. Configuring Redis to Handle the Complex Keys
Add this configuration to your Redis config class:

@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public KeyGenerator customKeyGenerator() {
        return new CustomKeyGenerator();
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
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

4. Using the Custom Key Generator in Your Service

@Service
public class DataService {
    
    // Using the custom key generator
    @Cacheable(value = "dataCache", keyGenerator = "customKeyGenerator")
    public List<Data> getData(FilterCriteria criteria) {
        // Fetch and return data
    }
    
    // Alternative: Using SpEL expression directly
    @Cacheable(value = "dataCache", 
               key = "'data:' + #criteria.countries + ':' + #criteria.segments + ':' + #criteria.regions")
    public List<Data> getDataAlternative(FilterCriteria criteria) {
        // Fetch and return data
    }
}
5. Additional Best Practices for Complex Keys

Consider key length and complexity:

Redis keys can be any binary sequence, but keep them reasonably short
Very long keys can impact memory usage and performance


Handle empty collections:

Decide how to represent empty arrays in your key (e.g., "EMPTY" or "_")
Be consistent with your approach

// Example handling for empty collections
private String formatCollection(List<String> collection) {
    if (collection == null || collection.isEmpty()) {
        return "EMPTY";
    }
    return StringUtils.collectionToDelimitedString(collection, "-");
}

Implement cache key validation:

Verify that generated keys conform to your naming conventions
Log unusually long or problematic keys during development


Consider using JSON for complex keys:

For very complex filter objects, serialize to JSON
Be aware this can create long keys

@Cacheable(value = "dataCache", key = "#root.method.name + ':' + @objectMapper.writeValueAsString(#criteria)")
public List<Data> getDataWithJsonKey(FilterCriteria criteria) {
    // Method implementation
}

This approach gives you a robust system for handling complex objects as cache keys in Redis with Spring Boot, allowing you to effectively cache data based on multiple filter criteria.

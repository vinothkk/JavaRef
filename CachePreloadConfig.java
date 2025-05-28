// 1. Create a Custom Annotation to Mark Services for Preloading
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePreloadable {
    /**
     * Priority for preloading (lower number = higher priority)
     */
    int priority() default 100;
    
    /**
     * Whether this service should be preloaded
     */
    boolean enabled() default true;
    
    /**
     * Description of what this service preloads
     */
    String description() default "";
}

// 2. Create an Interface for Preloadable Services
public interface PreloadableService {
    /**
     * This method will be called during cache preloading
     * Each service implements its own preloading logic
     */
    void preloadCache() throws Exception;
    
    /**
     * Return the name/identifier of this service for logging
     */
    default String getServiceName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Estimated time for preloading (in milliseconds) - for monitoring
     */
    default long getEstimatedPreloadTime() {
        return 1000; // Default 1 second
    }
}

// 3. Updated ProductServicesServiceImpl implementing PreloadableService
@Service
@CachePreloadable(priority = 1, description = "Product Services Cache Preloading")
public class ProductServicesServiceImpl implements ProductServicesService, PreloadableService {
    private static final Logger logger = LogManager.getLogger(ProductServicesServiceImpl.class);

    @Autowired
    ProductServicesRepository repository;
    
    @Autowired
    HierarchyService hierarchyService;

    @Autowired
    Environment environment;
    
    @Autowired
    private ProductServicesService self;

    // Your existing methods...
    public List<ProductServiceHierarchyRecordDTO> getProductServicesData(GlobalFilterDTO globalFilterDTO)
            throws SQLException {
        try {
            return self.getProductServicesDataCached(globalFilterDTO);
        } catch (Exception e) {
            logger.warn("Redis cache failed, falling back to direct database call: {}", e.getMessage());
            return repository.getProductServicesOptimizationList(globalFilterDTO);
        }
    }

    @Override
    @Cacheable(value = "getProductServicesOptimization", keyGenerator = "customKeyGenerator")
    public List<ProductServiceHierarchyRecordDTO> getProductServicesDataCached(GlobalFilterDTO globalFilterDTO) 
            throws SQLException {
        logger.info("Cache MISS - Fetching from database for preload/normal operation");
        return repository.getProductServicesOptimizationList(globalFilterDTO);
    }

    // Implement PreloadableService
    @Override
    public void preloadCache() throws Exception {
        logger.info("Starting ProductServices cache preload...");
        
        // Define common scenarios for this service
        List<GlobalFilterDTO> commonScenarios = getCommonFilterScenarios();
        
        for (GlobalFilterDTO filterDTO : commonScenarios) {
            try {
                logger.debug("Preloading ProductServices cache for scenario: {}", filterDTO.toString());
                getProductServicesDataCached(filterDTO);
                Thread.sleep(50); // Small delay
            } catch (Exception e) {
                logger.warn("Failed to preload ProductServices cache for scenario: {}", e.getMessage());
            }
        }
        
        logger.info("Completed ProductServices cache preload");
    }
    
    private List<GlobalFilterDTO> getCommonFilterScenarios() {
        List<GlobalFilterDTO> scenarios = new ArrayList<>();
        // Add your common scenarios
        GlobalFilterDTO defaultFilter = new GlobalFilterDTO();
        scenarios.add(defaultFilter);
        return scenarios;
    }
    
    @Override
    public long getEstimatedPreloadTime() {
        return 2000; // 2 seconds estimated
    }

    // Your other existing methods...
}

// 4. Example of Another Service with Cache Preloading
@Service
@CachePreloadable(priority = 2, description = "User Services Cache Preloading")
public class UserServicesServiceImpl implements UserServicesService, PreloadableService {
    private static final Logger logger = LogManager.getLogger(UserServicesServiceImpl.class);
    
    @Override
    public void preloadCache() throws Exception {
        logger.info("Starting UserServices cache preload...");
        // Implement user service specific preloading logic
        preloadActiveUsers();
        preloadUserRoles();
        logger.info("Completed UserServices cache preload");
    }
    
    private void preloadActiveUsers() throws Exception {
        // Your user-specific preloading logic
    }
    
    private void preloadUserRoles() throws Exception {
        // Your role-specific preloading logic
    }
    
    @Override
    public long getEstimatedPreloadTime() {
        return 1500; // 1.5 seconds estimated
    }
}

// 5. Example of Service NOT marked for preloading
@Service
// No @CachePreloadable annotation - will be skipped
public class ReportingServiceImpl implements ReportingService {
    // This service won't be preloaded
}

// 6. Updated CachePreloadService - Now Generic and Scalable
@Service
public class CachePreloadService {
    private static final Logger logger = LogManager.getLogger(CachePreloadService.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private Environment environment;
    
    @EventListener(ApplicationReadyEvent.class)
    public void preloadCache() {
        logger.info("Starting cache preload process...");
        
        try {
            boolean cachePreloadEnabled = Boolean.parseBoolean(
                environment.getProperty("cache.preload.enabled", "true")
            );
            
            if (!cachePreloadEnabled) {
                logger.info("Cache preload is disabled via configuration");
                return;
            }
            
            // Get all services marked for preloading
            List<PreloadableService> preloadableServices = getPreloadableServices();
            
            if (preloadableServices.isEmpty()) {
                logger.info("No services found for cache preloading");
                return;
            }
            
            logger.info("Found {} services for cache preloading", preloadableServices.size());
            
            // Execute preloading
            executePreloading(preloadableServices);
            
            logger.info("Cache preload process completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during cache preload: {}", e.getMessage(), e);
        }
    }
    
    private List<PreloadableService> getPreloadableServices() {
        List<PreloadableService> services = new ArrayList<>();
        
        // Get all beans that implement PreloadableService
        Map<String, PreloadableService> preloadableBeans = 
            applicationContext.getBeansOfType(PreloadableService.class);
        
        for (Map.Entry<String, PreloadableService> entry : preloadableBeans.entrySet()) {
            PreloadableService service = entry.getValue();
            Class<?> serviceClass = service.getClass();
            
            // Check if the service is marked with @CachePreloadable
            CachePreloadable annotation = serviceClass.getAnnotation(CachePreloadable.class);
            
            if (annotation != null && annotation.enabled()) {
                services.add(service);
                logger.info("Registered service for preloading: {} (Priority: {}, Description: {})", 
                    service.getServiceName(), annotation.priority(), annotation.description());
            }
        }
        
        // Sort by priority (lower number = higher priority)
        services.sort((s1, s2) -> {
            CachePreloadable ann1 = s1.getClass().getAnnotation(CachePreloadable.class);
            CachePreloadable ann2 = s2.getClass().getAnnotation(CachePreloadable.class);
            return Integer.compare(ann1.priority(), ann2.priority());
        });
        
        return services;
    }
    
    private void executePreloading(List<PreloadableService> services) {
        long totalStartTime = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;
        
        for (PreloadableService service : services) {
            try {
                long serviceStartTime = System.currentTimeMillis();
                
                logger.info("Starting cache preload for service: {}", service.getServiceName());
                
                // Execute preloading with timeout protection
                executeWithTimeout(service);
                
                long serviceEndTime = System.currentTimeMillis();
                long actualTime = serviceEndTime - serviceStartTime;
                
                logger.info("Completed cache preload for service: {} in {}ms", 
                    service.getServiceName(), actualTime);
                
                successCount++;
                
                // Add delay between services to avoid overwhelming the system
                Thread.sleep(200);
                
            } catch (Exception e) {
                logger.error("Failed to preload cache for service: {} - Error: {}", 
                    service.getServiceName(), e.getMessage());
                failureCount++;
            }
        }
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        logger.info("Cache preload summary - Total time: {}ms, Success: {}, Failures: {}", 
            totalTime, successCount, failureCount);
    }
    
    private void executeWithTimeout(PreloadableService service) throws Exception {
        long timeout = Math.max(service.getEstimatedPreloadTime() * 3, 30000); // 3x estimated or 30s min
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                service.preloadCache();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        future.get(timeout, TimeUnit.MILLISECONDS);
    }
}

// 7. Configuration for Selective Preloading
@Configuration
@ConfigurationProperties(prefix = "cache.preload")
public class CachePreloadConfig {
    private boolean enabled = true;
    private long timeoutMultiplier = 3;
    private long minTimeout = 30000; // 30 seconds
    private List<String> excludeServices = new ArrayList<>();
    private List<String> includeOnlyServices = new ArrayList<>();
    
    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public long getTimeoutMultiplier() {
        return timeoutMultiplier;
    }
    
    public void setTimeoutMultiplier(long timeoutMultiplier) {
        this.timeoutMultiplier = timeoutMultiplier;
    }
    
    public long getMinTimeout() {
        return minTimeout;
    }
    
    public void setMinTimeout(long minTimeout) {
        this.minTimeout = minTimeout;
    }
    
    public List<String> getExcludeServices() {
        return excludeServices;
    }
    
    public void setExcludeServices(List<String> excludeServices) {
        this.excludeServices = excludeServices;
    }
    
    public List<String> getIncludeOnlyServices() {
        return includeOnlyServices;
    }
    
    public void setIncludeOnlyServices(List<String> includeOnlyServices) {
        this.includeOnlyServices = includeOnlyServices;
    }
}

// 8. Configuration Properties Example
/*
# application.yml
cache:
  preload:
    enabled: true
    timeout-multiplier: 3
    min-timeout: 30000
    exclude-services:
      - "ReportingServiceImpl"
      - "AnalyticsServiceImpl"
    include-only-services: []  # If specified, only these services will be preloaded

# OR in application.properties
cache.preload.enabled=true
cache.preload.timeout-multiplier=3
cache.preload.min-timeout=30000
cache.preload.exclude-services=ReportingServiceImpl,AnalyticsServiceImpl
*/

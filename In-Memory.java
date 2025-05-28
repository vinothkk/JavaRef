// 1. Updated Service Implementation with In-Memory Filtering
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

    // Cache key for the complete dataset (no filters)
    private static final String COMPLETE_DATA_CACHE_KEY = "product_services_complete_data";
    
    public List<ProductServiceHierarchyRecordDTO> getProductServicesData(GlobalFilterDTO globalFilterDTO)
            throws SQLException {
        try {
            // First, try to get complete data from cache and filter in-memory
            return getFilteredDataFromCache(globalFilterDTO);
        } catch (Exception e) {
            logger.warn("Redis cache failed, falling back to direct database call: {}", e.getMessage());
            // Fallback to normal flow without cache
            return repository.getProductServicesOptimizationList(globalFilterDTO);
        }
    }

    /**
     * New method: Get complete data from cache and filter in-memory
     */
    private List<ProductServiceHierarchyRecordDTO> getFilteredDataFromCache(GlobalFilterDTO globalFilterDTO) 
            throws SQLException {
        
        // Get the complete dataset from cache (or database if cache miss)
        List<ProductServiceHierarchyRecordDTO> completeData = self.getCompleteDataCached();
        
        if (completeData == null || completeData.isEmpty()) {
            logger.warn("No complete data available in cache, falling back to filtered database query");
            return repository.getProductServicesOptimizationList(globalFilterDTO);
        }
        
        // Filter the complete data based on GlobalFilterDTO
        List<ProductServiceHierarchyRecordDTO> filteredData = filterDataInMemory(completeData, globalFilterDTO);
        
        logger.info("Filtered {} records from {} total records in-memory", 
            filteredData.size(), completeData.size());
        
        return filteredData;
    }

    /**
     * Cache the complete dataset without any filters
     */
    @Cacheable(value = "product_services_complete", key = "'complete_dataset'")
    public List<ProductServiceHierarchyRecordDTO> getCompleteDataCached() throws SQLException {
        logger.info("Cache MISS - Fetching complete dataset from database");
        
        // Create an empty filter to get all data
        GlobalFilterDTO emptyFilter = new GlobalFilterDTO();
        // Set any default values needed to get ALL data
        
        return repository.getProductServicesOptimizationList(emptyFilter);
    }

    /**
     * In-memory filtering logic
     */
    private List<ProductServiceHierarchyRecordDTO> filterDataInMemory(
            List<ProductServiceHierarchyRecordDTO> completeData, 
            GlobalFilterDTO filter) {
        
        return completeData.stream()
            .filter(record -> matchesFilter(record, filter))
            .collect(Collectors.toList());
    }

    /**
     * Filter matching logic - customize based on your GlobalFilterDTO fields
     */
    private boolean matchesFilter(ProductServiceHierarchyRecordDTO record, GlobalFilterDTO filter) {
        // Handle null filter (return all data)
        if (filter == null) {
            return true;
        }
        
        // Client Name filter
        if (filter.getClientName() != null && !filter.getClientName().trim().isEmpty()) {
            if (record.clientName() == null || 
                !record.clientName().toLowerCase().contains(filter.getClientName().toLowerCase().trim())) {
                return false;
            }
        }
        
        // Segment filter
        if (filter.getSegment() != null && !filter.getSegment().trim().isEmpty()) {
            if (record.segment() == null || 
                !record.segment().equalsIgnoreCase(filter.getSegment().trim())) {
                return false;
            }
        }
        
        // Region filter
        if (filter.getRegion() != null && !filter.getRegion().trim().isEmpty()) {
            if (record.region() == null || 
                !record.region().equalsIgnoreCase(filter.getRegion().trim())) {
                return false;
            }
        }
        
        // Customer Name filter
        if (filter.getCustomerName() != null && !filter.getCustomerName().trim().isEmpty()) {
            if (record.customerName() == null || 
                !record.customerName().toLowerCase().contains(filter.getCustomerName().toLowerCase().trim())) {
                return false;
            }
        }
        
        // Parent ID filter
        if (filter.getParentId() != null && !filter.getParentId().trim().isEmpty()) {
            if (record.parentId() == null || 
                !record.parentId().equalsIgnoreCase(filter.getParentId().trim())) {
                return false;
            }
        }
        
        // Parent Name filter
        if (filter.getParentName() != null && !filter.getParentName().trim().isEmpty()) {
            if (record.parentName() == null || 
                !record.parentName().toLowerCase().contains(filter.getParentName().toLowerCase().trim())) {
                return false;
            }
        }
        
        // MDM Client GEMS ID filter
        if (filter.getMdmClientGemsId() != null && !filter.getMdmClientGemsId().trim().isEmpty()) {
            if (record.mdmClientGemsId() == null || 
                !record.mdmClientGemsId().equalsIgnoreCase(filter.getMdmClientGemsId().trim())) {
                return false;
            }
        }
        
        // MDM Customer GEMS ID filter
        if (filter.getMdmCustGemsId() != null && !filter.getMdmCustGemsId().trim().isEmpty()) {
            if (record.mdmCustGemsId() == null || 
                !record.mdmCustGemsId().equalsIgnoreCase(filter.getMdmCustGemsId().trim())) {
                return false;
            }
        }
        
        // Add more filter conditions as needed based on your GlobalFilterDTO fields
        // For example, if you have date ranges, numeric ranges, etc.
        
        return true; // Record matches all filter criteria
    }

    // Keep the old method for backward compatibility or fallback
    @Cacheable(value = "getProductServicesOptimization", keyGenerator = "customKeyGenerator")
    public List<ProductServiceHierarchyRecordDTO> getProductServicesDataCached(GlobalFilterDTO globalFilterDTO) 
            throws SQLException {
        logger.info("Cache MISS - Fetching filtered data from database (fallback method)");
        return repository.getProductServicesOptimizationList(globalFilterDTO);
    }

    @Override
    public ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> getProductServicesOptimization(
            GlobalFilterDTO globalFilterDTO) {
        logger.info("ProductServicesServiceImpl: getProductServicesOptimization()");
        List<ProductServiceHierarchyRecordDTO> productServicesDTOOptimization;
        List<ParentDataRecordDTO> ParentDataRecordResponse;
        
        try {
            productServicesDTOOptimization = getProductServicesData(globalFilterDTO);
            ParentDataRecordResponse = hierarchyService.buildHierarchy(productServicesDTOOptimization);
        } catch (SQLException e) {
            logger.error("SQLException: {}", e.getMessage());
            return new ResponseEntity<>(
                ResponseData.failure(Constants.INTERNAL_SERVER_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR));
        } catch (Exception e) {
            logger.error("Exception: {}", e.getMessage());
            return new ResponseEntity<>(
                ResponseData.failure(Constants.INTERNAL_SERVER_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR));
        }
        
        return new ResponseEntity<>(
            ResponseData.success(ParentDataRecordResponse, Constants.SUCCESS_MESSAGE, HttpStatus.OK));
    }

    // Implement PreloadableService
    @Override
    public void preloadCache() throws Exception {
        logger.info("Starting ProductServices cache preload (complete dataset)...");
        
        try {
            // Preload the complete dataset
            List<ProductServiceHierarchyRecordDTO> completeData = getCompleteDataCached();
            logger.info("Preloaded complete dataset with {} records", completeData.size());
            
        } catch (Exception e) {
            logger.warn("Failed to preload ProductServices complete dataset: {}", e.getMessage());
            throw e;
        }
        
        logger.info("Completed ProductServices cache preload");
    }
    
    @Override
    public long getEstimatedPreloadTime() {
        return 5000; // 5 seconds estimated for complete dataset
    }
}

// 2. Advanced Filtering Utility Class
@Component
public class InMemoryFilterUtil {
    private static final Logger logger = LogManager.getLogger(InMemoryFilterUtil.class);

    /**
     * Generic filtering method with performance optimization
     */
    public <T> List<T> filterRecords(List<T> records, GlobalFilterDTO filter, 
                                   Function<T, Boolean> filterFunction) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (filter == null || isEmptyFilter(filter)) {
            return new ArrayList<>(records); // Return copy of original list
        }
        
        long startTime = System.currentTimeMillis();
        
        List<T> filteredRecords = records.parallelStream() // Use parallel stream for large datasets
            .filter(record -> filterFunction.apply(record))
            .collect(Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        
        logger.debug("Filtered {} records from {} in {}ms", 
            filteredRecords.size(), records.size(), (endTime - startTime));
        
        return filteredRecords;
    }
    
    /**
     * Check if filter is empty/null
     */
    public boolean isEmptyFilter(GlobalFilterDTO filter) {
        if (filter == null) return true;
        
        return (filter.getClientName() == null || filter.getClientName().trim().isEmpty()) &&
               (filter.getSegment() == null || filter.getSegment().trim().isEmpty()) &&
               (filter.getRegion() == null || filter.getRegion().trim().isEmpty()) &&
               (filter.getCustomerName() == null || filter.getCustomerName().trim().isEmpty()) &&
               (filter.getParentId() == null || filter.getParentId().trim().isEmpty()) &&
               (filter.getParentName() == null || filter.getParentName().trim().isEmpty()) &&
               (filter.getMdmClientGemsId() == null || filter.getMdmClientGemsId().trim().isEmpty()) &&
               (filter.getMdmCustGemsId() == null || filter.getMdmCustGemsId().trim().isEmpty());
               // Add other filter fields as needed
    }
    
    /**
     * Create filter predicates for complex filtering scenarios
     */
    public Predicate<ProductServiceHierarchyRecordDTO> createFilterPredicate(GlobalFilterDTO filter) {
        List<Predicate<ProductServiceHierarchyRecordDTO>> predicates = new ArrayList<>();
        
        // Client Name filter
        if (filter.getClientName() != null && !filter.getClientName().trim().isEmpty()) {
            predicates.add(record -> record.clientName() != null && 
                record.clientName().toLowerCase().contains(filter.getClientName().toLowerCase().trim()));
        }
        
        // Segment filter
        if (filter.getSegment() != null && !filter.getSegment().trim().isEmpty()) {
            predicates.add(record -> record.segment() != null && 
                record.segment().equalsIgnoreCase(filter.getSegment().trim()));
        }
        
        // Region filter
        if (filter.getRegion() != null && !filter.getRegion().trim().isEmpty()) {
            predicates.add(record -> record.region() != null && 
                record.region().equalsIgnoreCase(filter.getRegion().trim()));
        }
        
        // Combine all predicates with AND logic
        return predicates.stream().reduce(Predicate::and).orElse(record -> true);
    }
}

// 3. Enhanced Cache Configuration with TTL and Memory Management
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(6)) // Complete dataset cache for 6 hours
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Different TTL for different caches
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Complete dataset - longer TTL
        cacheConfigurations.put("product_services_complete", 
            config.entryTtl(Duration.ofHours(12)));
        
        // Filtered results - shorter TTL (if you still want to cache some specific filters)
        cacheConfigurations.put("getProductServicesOptimization", 
            config.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

// 4. Cache Statistics and Monitoring
@Component
public class CacheStatsService {
    private static final Logger logger = LogManager.getLogger(CacheStatsService.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    public void logCacheStats() {
        Cache completeDataCache = cacheManager.getCache("product_services_complete");
        if (completeDataCache != null) {
            // Log cache hit/miss statistics
            logger.info("Complete data cache statistics available");
        }
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logCacheStatistics() {
        logCacheStats();
    }
}

package com.example.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A single utility class for all SQL filter operations.
 * This class can be used directly without needing multiple services.
 */
@Component
public class QueryFilterUtil {
    private static final Logger logger = LoggerFactory.getLogger(QueryFilterUtil.class);

    /**
     * Builds a complete SQL query by applying filters from GlobalFilterDTO
     * 
     * @param baseQuery The base SQL query (with or without WHERE clause)
     * @param filterDTO The DTO containing filter values
     * @return Complete SQL query with filters applied
     */
    public String buildFilteredQuery(String baseQuery, GlobalFilterDTO filterDTO) {
        if (filterDTO == null) {
            return baseQuery;
        }

        List<String> conditions = new ArrayList<>();
        
        // Client filter
        if (filterDTO.getClient() != null && !filterDTO.getClient().isEmpty()) {
            conditions.add("mdm_gems_ult_parent_id in (" + 
                filterDTO.getClient().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")) + 
                ")");
        }
        
        // Segment filter
        if (filterDTO.getSegment() != null && !filterDTO.getSegment().isEmpty()) {
            conditions.add("mdm_client_segment in (" + 
                filterDTO.getSegment().stream()
                    .map(s -> "'" + s.replace("'", "''") + "'")
                    .collect(Collectors.joining(",")) + 
                ")");
        }
        
        // Region filter
        if (filterDTO.getRegion() != null && !filterDTO.getRegion().isEmpty()) {
            conditions.add("region_cd in (" + 
                filterDTO.getRegion().stream()
                    .map(r -> "'" + r.replace("'", "''") + "'")
                    .collect(Collectors.joining(",")) + 
                ")");
        }
        
        // Country filter
        if (filterDTO.getCountry() != null && !filterDTO.getCountry().isEmpty()) {
            conditions.add("country_cd in (" + 
                filterDTO.getCountry().stream()
                    .map(c -> "'" + c.replace("'", "''") + "'")
                    .collect(Collectors.joining(",")) + 
                ")");
        }
        
        // If no conditions, return the base query
        if (conditions.isEmpty()) {
            return baseQuery;
        }
        
        // Combine all conditions
        String whereClause = String.join(" AND ", conditions);
        
        // Check if base query already has WHERE clause
        if (baseQuery.toLowerCase().contains(" where ")) {
            return baseQuery + " AND " + whereClause;
        } else {
            return baseQuery + " WHERE " + whereClause;
        }
    }
    
    /**
     * More flexible method that accepts a Map of filter parameters
     * Can be used for any column names and filter values
     * 
     * @param baseQuery The base SQL query
     * @param filterMap Map of column names to filter values
     * @return Complete SQL query with filters applied
     */
    public String buildDynamicQuery(String baseQuery, Map<String, Object> filterMap) {
        if (filterMap == null || filterMap.isEmpty()) {
            return baseQuery;
        }
        
        List<String> conditions = new ArrayList<>();
        
        filterMap.forEach((column, value) -> {
            if (value != null) {
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    if (!values.isEmpty()) {
                        conditions.add(buildInClause(column, values));
                    }
                } else if (value instanceof String) {
                    // For string values, add single quotes
                    String strValue = (String) value;
                    conditions.add(column + " = '" + strValue.replace("'", "''") + "'");
                } else {
                    // For numeric values, no quotes
                    conditions.add(column + " = " + value);
                }
            }
        });
        
        if (conditions.isEmpty()) {
            return baseQuery;
        }
        
        String whereClause = String.join(" AND ", conditions);
        
        // Check if base query already has WHERE clause
        if (baseQuery.toLowerCase().contains(" where ")) {
            return baseQuery + " AND " + whereClause;
        } else {
            return baseQuery + " WHERE " + whereClause;
        }
    }
    
    /**
     * Helper method to build IN clause based on value type
     */
    private String buildInClause(String column, List<?> values) {
        if (values.isEmpty()) {
            return "";
        }
        
        // Check the type of the first element to determine if we need quotes
        Object firstElement = values.get(0);
        if (firstElement instanceof String) {
            // For string values, add single quotes
            String inValues = values.stream()
                .map(v -> "'" + ((String) v).replace("'", "''") + "'")
                .collect(Collectors.joining(","));
            return column + " in (" + inValues + ")";
        } else {
            // For numeric values, no quotes
            String inValues = values.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
            return column + " in (" + inValues + ")";
        }
    }
    
    /**
     * Converts a GlobalFilterDTO to a Map that can be used with buildDynamicQuery
     * This is useful if you need to add additional custom filters
     */
    public Map<String, Object> convertDtoToMap(GlobalFilterDTO filterDTO) {
        Map<String, Object> filterMap = new java.util.HashMap<>();
        
        if (filterDTO.getClient() != null && !filterDTO.getClient().isEmpty()) {
            filterMap.put("mdm_gems_ult_parent_id", filterDTO.getClient());
        }
        
        if (filterDTO.getSegment() != null && !filterDTO.getSegment().isEmpty()) {
            filterMap.put("mdm_client_segment", filterDTO.getSegment());
        }
        
        if (filterDTO.getRegion() != null && !filterDTO.getRegion().isEmpty()) {
            filterMap.put("region_cd", filterDTO.getRegion());
        }
        
        if (filterDTO.getCountry() != null && !filterDTO.getCountry().isEmpty()) {
            filterMap.put("country_cd", filterDTO.getCountry());
        }
        
        return filterMap;
    }
    
    /**
     * Add a LIKE condition to a filter map (for partial string matching)
     */
    public void addLikeCondition(Map<String, Object> filterMap, String column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            // Add % wildcards for partial matching
            filterMap.put(column + " LIKE ", "%" + value.replace("'", "''") + "%");
        }
    }
    
    /**
     * Add a comparison condition to a filter map
     */
    public void addComparisonCondition(Map<String, Object> filterMap, String column, String operator, Object value) {
        if (value != null) {
            filterMap.put(column + " " + operator + " ", value);
        }
    }
}

===========================
package com.example.filters;

import java.util.List;

/**
 * Data Transfer Object for global filtering criteria
 */
public class GlobalFilterDTO {
    private List<Integer> client;
    private List<String> segment;
    private List<String> region;
    private List<String> country;
    
    // Default constructor
    public GlobalFilterDTO() {
    }
    
    // Getters and setters
    public List<Integer> getClient() {
        return client;
    }
    
    public void setClient(List<Integer> client) {
        this.client = client;
    }
    
    public List<String> getSegment() {
        return segment;
    }
    
    public void setSegment(List<String> segment) {
        this.segment = segment;
    }
    
    public List<String> getRegion() {
        return region;
    }
    
    public void setRegion(List<String> region) {
        this.region = region;
    }
    
    public List<String> getCountry() {
        return country;
    }
    
    public void setCountry(List<String> country) {
        this.country = country;
    }
    
    @Override
    public String toString() {
        return "GlobalFilterDTO{" +
                "client=" + client +
                ", segment=" + segment +
                ", region=" + region +
                ", country=" + country +
                '}';
    }
}
==========================================
package com.example.demo;

import com.example.filters.GlobalFilterDTO;
import com.example.filters.QueryFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example controller showing how to use the QueryFilterUtil
 */
@RestController
public class QueryFilterUsageExample {

    private final QueryFilterUtil queryFilterUtil;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public QueryFilterUsageExample(QueryFilterUtil queryFilterUtil, JdbcTemplate jdbcTemplate) {
        this.queryFilterUtil = queryFilterUtil;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Basic example using GlobalFilterDTO
     */
    @PostMapping("/clients")
    public List<Map<String, Object>> getClients(@RequestBody GlobalFilterDTO filterDTO) {
        String baseQuery = "SELECT client_id, client_name FROM client_table";
        String finalQuery = queryFilterUtil.buildFilteredQuery(baseQuery, filterDTO);
        
        // Log the generated query for debugging
        System.out.println("Executing query: " + finalQuery);
        
        return jdbcTemplate.queryForList(finalQuery);
    }
    
    /**
     * Example of using the utility with Map-based filters for more flexibility
     */
    @PostMapping("/advanced-search")
    public List<Map<String, Object>> advancedSearch(@RequestBody Map<String, Object> requestParams) {
        Map<String, Object> filterMap = new HashMap<>();
        
        // Extract and transform filter parameters as needed
        if (requestParams.containsKey("clientIds")) {
            filterMap.put("mdm_gems_ult_parent_id", requestParams.get("clientIds"));
        }
        
        if (requestParams.containsKey("segments")) {
            filterMap.put("mdm_client_segment", requestParams.get("segments"));
        }
        
        if (requestParams.containsKey("regions")) {
            filterMap.put("region_cd", requestParams.get("regions"));
        }
        
        if (requestParams.containsKey("countries")) {
            filterMap.put("country_cd", requestParams.get("countries"));
        }
        
        // Add a LIKE condition for name search
        if (requestParams.containsKey("nameSearch")) {
            queryFilterUtil.addLikeCondition(filterMap, "client_name", 
                                            (String) requestParams.get("nameSearch"));
        }
        
        // Add a comparison for revenue
        if (requestParams.containsKey("minRevenue")) {
            queryFilterUtil.addComparisonCondition(filterMap, "annual_revenue", ">=", 
                                                  requestParams.get("minRevenue"));
        }
        
        String baseQuery = "SELECT * FROM client_table";
        String finalQuery = queryFilterUtil.buildDynamicQuery(baseQuery, filterMap);
        
        return jdbcTemplate.queryForList(finalQuery);
    }
    
    /**
     * Example of programmatically creating a filter DTO
     */
    @PostMapping("/sample-filter")
    public List<Map<String, Object>> getSampleFilteredData() {
        // Create filter DTO programmatically
        GlobalFilterDTO filterDTO = new GlobalFilterDTO();
        filterDTO.setClient(Arrays.asList(1, 3, 4, 5));
        filterDTO.setSegment(Arrays.asList("NA", "ECA"));
        filterDTO.setRegion(Arrays.asList("US", "UK", "Russia"));
        
        String baseQuery = "SELECT * FROM client_table";
        String finalQuery = queryFilterUtil.buildFilteredQuery(baseQuery, filterDTO);
        
        return jdbcTemplate.queryForList(finalQuery);
    }
    
    /**
     * Example of combining both approaches
     */
    @PostMapping("/combined-search")
    public List<Map<String, Object>> combinedSearch(@RequestBody GlobalFilterDTO filterDTO) {
        // Convert the standard DTO to a Map
        Map<String, Object> filterMap = queryFilterUtil.convertDtoToMap(filterDTO);
        
        // Add additional custom filters
        queryFilterUtil.addLikeCondition(filterMap, "client_name", "Company");
        queryFilterUtil.addComparisonCondition(filterMap, "created_date", ">", "2023-01-01");
        
        // Build and execute the query
        String baseQuery = "SELECT * FROM client_table";
        String finalQuery = queryFilterUtil.buildDynamicQuery(baseQuery, filterMap);
        
        return jdbcTemplate.queryForList(finalQuery);
    }
}
================================================================
package com.example.demo;

import com.example.filters.GlobalFilterDTO;
import com.example.filters.QueryFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Example showing how to use QueryFilterUtil in a service or repository class
 * with conditional logic
 */
@Service
public class ConditionalFilterExample {

    private final QueryFilterUtil queryFilterUtil;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public ConditionalFilterExample(QueryFilterUtil queryFilterUtil, JdbcTemplate jdbcTemplate) {
        this.queryFilterUtil = queryFilterUtil;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Example method showing how to apply filters with conditional logic
     */
    public List<Map<String, Object>> getClientDataWithDynamicFilters(GlobalFilterDTO filterDTO, 
                                                                   String namePattern,
                                                                   Boolean activeOnly) {
        // Base query
        String baseQuery = "SELECT * FROM client_table";
        
        // First, apply the standard filters from the DTO
        String query = queryFilterUtil.buildFilteredQuery(baseQuery, filterDTO);
        
        // Then, conditionally add more filters using direct string manipulation
        // (for simple cases where the utility methods might be overkill)
        if (namePattern != null && !namePattern.isEmpty()) {
            String safePattern = namePattern.replace("'", "''");
            if (query.toLowerCase().contains(" where ")) {
                query += " AND client_name LIKE '%" + safePattern + "%'";
            } else {
                query += " WHERE client_name LIKE '%" + safePattern + "%'";
            }
        }
        
        if (activeOnly != null && activeOnly) {
            if (query.toLowerCase().contains(" where ")) {
                query += " AND active_flag = 'Y'";
            } else {
                query += " WHERE active_flag = 'Y'";
            }
        }
        
        // Alternative approach using Map
        // This is cleaner for multiple conditional filters
        /*
        Map<String, Object> additionalFilters = new HashMap<>();
        
        if (namePattern != null && !namePattern.isEmpty()) {
            queryFilterUtil.addLikeCondition(additionalFilters, "client_name", namePattern);
        }
        
        if (activeOnly != null && activeOnly) {
            additionalFilters.put("active_flag", "Y");
        }
        
        if (!additionalFilters.isEmpty()) {
            // Apply additional filters
            query = queryFilterUtil.buildDynamicQuery(query, additionalFilters);
        }
        */
        
        // Execute the query
        return jdbcTemplate.queryForList(query);
    }
    
    /**
     * Example showing how to handle a query that might not need filtering at all
     */
    public List<Map<String, Object>> getReportData(GlobalFilterDTO filterDTO) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT c.client_name, SUM(t.transaction_amount) as total_amount ");
        queryBuilder.append("FROM client_table c ");
        queryBuilder.append("JOIN transaction_table t ON c.client_id = t.client_id ");
        queryBuilder.append("WHERE t.transaction_date >= '2023-01-01' ");
        queryBuilder.append("GROUP BY c.client_name ");
        queryBuilder.append("ORDER BY total_amount DESC");
        
        String baseQuery = queryBuilder.toString();
        
        // Only apply filters if the DTO is not null and has at least one non-empty filter
        if (filterDTO != null && (
                (filterDTO.getClient() != null && !filterDTO.getClient().isEmpty()) ||
                (filterDTO.getSegment() != null && !filterDTO.getSegment().isEmpty()) ||
                (filterDTO.getRegion() != null && !filterDTO.getRegion().isEmpty()) ||
                (filterDTO.getCountry() != null && !filterDTO.getCountry().isEmpty())
            )) {
            // Query already has a WHERE clause, so filters will be added with AND
            String finalQuery = queryFilterUtil.buildFilteredQuery(baseQuery, filterDTO);
            return jdbcTemplate.queryForList(finalQuery);
        } else {
            // No filtering needed
            return jdbcTemplate.queryForList(baseQuery);
        }
    }
}
=====================================================================
How to Use It
Basic Usage with DTO
// In your service or controller
@Autowired
private QueryFilterUtil queryFilterUtil;
@Autowired
private JdbcTemplate jdbcTemplate;

public List<Map<String, Object>> getClients(GlobalFilterDTO filterDTO) {
    String baseQuery = "SELECT client_id, client_name FROM client_table";
    String finalQuery = queryFilterUtil.buildFilteredQuery(baseQuery, filterDTO);
    return jdbcTemplate.queryForList(finalQuery);
}
=====================
Advanced Usage with Additional Filters
===========================================
public List<Map<String, Object>> searchClients(GlobalFilterDTO filterDTO, String nameSearch) {
    // Start with the base filters from the DTO
    Map<String, Object> allFilters = queryFilterUtil.convertDtoToMap(filterDTO);
    
    // Add additional custom filters
    if (nameSearch != null && !nameSearch.isEmpty()) {
        queryFilterUtil.addLikeCondition(allFilters, "client_name", nameSearch);
    }
    
    // Build and execute the final query
    String baseQuery = "SELECT * FROM client_table";
    String finalQuery = queryFilterUtil.buildDynamicQuery(baseQuery, allFilters);
    return jdbcTemplate.queryForList(finalQuery);
}

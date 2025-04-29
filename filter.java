1
==========
package com.example.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GlobalFilterService {
    private static final Logger logger = LoggerFactory.getLogger(GlobalFilterService.class);

    /**
     * Applies global filter logic to create WHERE clause conditions based on the provided filters
     * @param globalFilterDTO The DTO containing filter values
     * @return A string containing the SQL WHERE clause conditions (without the "WHERE" keyword)
     */
    public String applyGlobalFilterLogic(GlobalFilterDTO globalFilterDTO) {
        List<String> conditions = new ArrayList<>();
        
        if (globalFilterDTO != null) {
            // Add client filter if provided
            if (globalFilterDTO.getClient() != null && !globalFilterDTO.getClient().isEmpty()) {
                String clientInClause = convertNumericListToStringInClause(globalFilterDTO.getClient());
                if (clientInClause != null && !clientInClause.isEmpty()) {
                    conditions.add("mdm_gems_ult_parent_id in (" + clientInClause + ")");
                    logger.info("Added client filter: mdm_gems_ult_parent_id in (" + clientInClause + ")");
                }
            }
            
            // Add segment filter if provided
            if (globalFilterDTO.getSegment() != null && !globalFilterDTO.getSegment().isEmpty()) {
                String segmentInClause = convertStringListToStringInClause(globalFilterDTO.getSegment());
                if (segmentInClause != null && !segmentInClause.isEmpty()) {
                    conditions.add("mdm_client_segment in (" + segmentInClause + ")");
                    logger.info("Added segment filter: mdm_client_segment in (" + segmentInClause + ")");
                }
            }
            
            // Add region filter if provided
            if (globalFilterDTO.getRegion() != null && !globalFilterDTO.getRegion().isEmpty()) {
                String regionInClause = convertStringListToStringInClause(globalFilterDTO.getRegion());
                if (regionInClause != null && !regionInClause.isEmpty()) {
                    conditions.add("region_cd in (" + regionInClause + ")");
                    logger.info("Added region filter: region_cd in (" + regionInClause + ")");
                }
            }
            
            // Add country filter if provided
            if (globalFilterDTO.getCountry() != null && !globalFilterDTO.getCountry().isEmpty()) {
                String countryInClause = convertStringListToStringInClause(globalFilterDTO.getCountry());
                if (countryInClause != null && !countryInClause.isEmpty()) {
                    conditions.add("country_cd in (" + countryInClause + ")");
                    logger.info("Added country filter: country_cd in (" + countryInClause + ")");
                }
            }
        }
        
        // Join all conditions with AND
        return conditions.isEmpty() ? "" : String.join(" AND ", conditions);
    }
    
    /**
     * Converts a list of numeric values to a comma-separated string for SQL IN clause
     * No quotes are added to numeric values
     */
    private String convertNumericListToStringInClause(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }
    
    /**
     * Converts a list of string values to a comma-separated string with quotes for SQL IN clause
     */
    private String convertStringListToStringInClause(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(v -> "'" + v.replace("'", "''") + "'") // Escape single quotes for SQL safety
                .collect(Collectors.joining(","));
    }
}
2
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
3
===============================================
package com.example.filters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to build SQL queries with dynamic filtering
 */
@Service
public class QueryBuilderService {

    private final GlobalFilterService globalFilterService;
    
    @Autowired
    public QueryBuilderService(GlobalFilterService globalFilterService) {
        this.globalFilterService = globalFilterService;
    }
    
    /**
     * Builds a complete SQL query with dynamic filters
     * 
     * @param baseQuery The base SQL query without WHERE clause
     * @param globalFilterDTO The filter criteria
     * @return Complete SQL query with appropriate WHERE clause
     */
    public String buildQuery(String baseQuery, GlobalFilterDTO globalFilterDTO) {
        String filterConditions = globalFilterService.applyGlobalFilterLogic(globalFilterDTO);
        
        if (filterConditions.isEmpty()) {
            return baseQuery;
        }
        
        // Check if the base query already has a WHERE clause
        if (baseQuery.toLowerCase().contains(" where ")) {
            return baseQuery + " AND " + filterConditions;
        } else {
            return baseQuery + " WHERE " + filterConditions;
        }
    }
    
    /**
     * Builds a count query with the same filter conditions
     * 
     * @param tableName The name of the table to count from
     * @param globalFilterDTO The filter criteria
     * @return SQL query to count filtered records
     */
    public String buildCountQuery(String tableName, GlobalFilterDTO globalFilterDTO) {
        String baseCountQuery = "SELECT COUNT(*) FROM " + tableName;
        return buildQuery(baseCountQuery, globalFilterDTO);
    }
}
4
===============================================
package com.example.controllers;

import com.example.filters.GlobalFilterDTO;
import com.example.filters.QueryBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Example controller showing how to use the QueryBuilderService
 */
@RestController
public class ClientController {

    private final QueryBuilderService queryBuilderService;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public ClientController(QueryBuilderService queryBuilderService, JdbcTemplate jdbcTemplate) {
        this.queryBuilderService = queryBuilderService;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @PostMapping("/clients")
    public List<Map<String, Object>> getClientsByFilter(@RequestBody GlobalFilterDTO globalFilterDTO) {
        // Base query
        String baseQuery = "SELECT client_id, client_name, mdm_client_segment, region_cd, country_cd " +
                           "FROM client_table";
        
        // Build the complete query with filters
        String finalQuery = queryBuilderService.buildQuery(baseQuery, globalFilterDTO);
        
        // Execute the query
        return jdbcTemplate.queryForList(finalQuery);
    }
    
    @PostMapping("/clients/count")
    public int getClientCount(@RequestBody GlobalFilterDTO globalFilterDTO) {
        String countQuery = queryBuilderService.buildCountQuery("client_table", globalFilterDTO);
        return jdbcTemplate.queryForObject(countQuery, Integer.class);
    }
    
    // Example of using the service with a more complex base query
    @PostMapping("/client-details")
    public List<Map<String, Object>> getClientDetails(@RequestBody GlobalFilterDTO globalFilterDTO) {
        String baseQuery = "SELECT c.client_id, c.client_name, c.mdm_client_segment, " +
                           "r.region_name, co.country_name " +
                           "FROM client_table c " +
                           "JOIN region_table r ON c.region_cd = r.region_cd " +
                           "JOIN country_table co ON c.country_cd = co.country_cd " +
                           "WHERE c.active_flag = 'Y'";  // Base query already has WHERE clause
        
        String finalQuery = queryBuilderService.buildQuery(baseQuery, globalFilterDTO);
        
        return jdbcTemplate.queryForList(finalQuery);
    }
}
5
==================================================================
package com.example.filters;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A more generic and flexible filter service that can handle any field and value type
 */
@Service
public class GenericFilterService {

    /**
     * Filter condition type enum (for different comparison operators)
     */
    public enum FilterType {
        EQUALS,
        IN,
        LIKE,
        GREATER_THAN,
        LESS_THAN,
        BETWEEN
    }

    /**
     * Builds a WHERE clause from a map of filter criteria
     * @param filters Map of column names to filter values
     * @return SQL WHERE clause without the "WHERE" keyword
     */
    public String buildWhereClause(Map<String, FilterCondition<?>> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }

        List<String> conditions = new ArrayList<>();

        filters.forEach((column, condition) -> {
            String sqlCondition = buildSqlCondition(column, condition);
            if (sqlCondition != null && !sqlCondition.isEmpty()) {
                conditions.add(sqlCondition);
            }
        });

        return String.join(" AND ", conditions);
    }

    /**
     * Builds an SQL condition for a single column based on filter type
     */
    private <T> String buildSqlCondition(String column, FilterCondition<T> condition) {
        if (condition == null || condition.getValue() == null) {
            return null;
        }

        switch (condition.getType()) {
            case EQUALS:
                return column + " = " + formatValue(condition.getValue());
                
            case IN:
                if (condition.getValue() instanceof List) {
                    List<?> values = (List<?>) condition.getValue();
                    if (values.isEmpty()) {
                        return null;
                    }
                    String inClause = values.stream()
                            .map(this::formatValue)
                            .collect(Collectors.joining(","));
                    return column + " IN (" + inClause + ")";
                }
                return null;
                
            case LIKE:
                return column + " LIKE " + formatValue(condition.getValue());
                
            case GREATER_THAN:
                return column + " > " + formatValue(condition.getValue());
                
            case LESS_THAN:
                return column + " < " + formatValue(condition.getValue());
                
            case BETWEEN:
                if (condition.getValue() instanceof List) {
                    List<?> values = (List<?>) condition.getValue();
                    if (values.size() >= 2) {
                        return column + " BETWEEN " + formatValue(values.get(0)) + 
                               " AND " + formatValue(values.get(1));
                    }
                }
                return null;
                
            default:
                return null;
        }
    }

    /**
     * Formats a value based on its type for use in SQL
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "0";
        }
        
        // For other types, convert to string and wrap in quotes
        return "'" + value.toString().replace("'", "''") + "'";
    }

    /**
     * Generic filter condition class
     */
    public static class FilterCondition<T> {
        private final FilterType type;
        private final T value;

        public FilterCondition(FilterType type, T value) {
            this.type = type;
            this.value = value;
        }

        public FilterType getType() {
            return type;
        }

        public T getValue() {
            return value;
        }
    }
}
6=====================================
package com.example.filters;

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
 * Example controller demonstrating the use of the generic filter service
 */
@RestController
public class GenericFilterExample {

    private final GenericFilterService filterService;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public GenericFilterExample(GenericFilterService filterService, JdbcTemplate jdbcTemplate) {
        this.filterService = filterService;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Example of converting a GlobalFilterDTO to generic filter conditions
     */
    @PostMapping("/generic-clients")
    public List<Map<String, Object>> getClientsGeneric(@RequestBody GlobalFilterDTO globalFilterDTO) {
        // Convert GlobalFilterDTO to generic filter map
        Map<String, GenericFilterService.FilterCondition<?>> filterMap = new HashMap<>();
        
        if (globalFilterDTO.getClient() != null && !globalFilterDTO.getClient().isEmpty()) {
            filterMap.put("mdm_gems_ult_parent_id", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.IN, globalFilterDTO.getClient()));
        }
        
        if (globalFilterDTO.getSegment() != null && !globalFilterDTO.getSegment().isEmpty()) {
            filterMap.put("mdm_client_segment", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.IN, globalFilterDTO.getSegment()));
        }
        
        if (globalFilterDTO.getRegion() != null && !globalFilterDTO.getRegion().isEmpty()) {
            filterMap.put("region_cd", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.IN, globalFilterDTO.getRegion()));
        }
        
        if (globalFilterDTO.getCountry() != null && !globalFilterDTO.getCountry().isEmpty()) {
            filterMap.put("country_cd", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.IN, globalFilterDTO.getCountry()));
        }
        
        // Build the query
        String baseQuery = "SELECT client_id, client_name FROM client_table";
        String whereClause = filterService.buildWhereClause(filterMap);
        String finalQuery = whereClause.isEmpty() ? baseQuery : baseQuery + " WHERE " + whereClause;
        
        return jdbcTemplate.queryForList(finalQuery);
    }
    
    /**
     * Example with complex filter criteria using different filter types
     */
    @PostMapping("/advanced-search")
    public List<Map<String, Object>> advancedSearch(@RequestBody Map<String, Object> requestBody) {
        Map<String, GenericFilterService.FilterCondition<?>> filterMap = new HashMap<>();
        
        // Example of various filter types
        if (requestBody.containsKey("nameSearch")) {
            String namePattern = "%" + requestBody.get("nameSearch") + "%";
            filterMap.put("client_name", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.LIKE, namePattern));
        }
        
        if (requestBody.containsKey("revenueMin")) {
            filterMap.put("annual_revenue", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.GREATER_THAN, requestBody.get("revenueMin")));
        }
        
        if (requestBody.containsKey("dateRange") && requestBody.get("dateRange") instanceof List) {
            filterMap.put("created_date", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.BETWEEN, requestBody.get("dateRange")));
        }
        
        if (requestBody.containsKey("status")) {
            filterMap.put("status", 
                new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.EQUALS, requestBody.get("status")));
        }
        
        String baseQuery = "SELECT * FROM client_table";
        String whereClause = filterService.buildWhereClause(filterMap);
        String finalQuery = whereClause.isEmpty() ? baseQuery : baseQuery + " WHERE " + whereClause;
        
        return jdbcTemplate.queryForList(finalQuery);
    }
}
==============================================
// Create a GlobalFilterDTO with your filter criteria
GlobalFilterDTO filterDTO = new GlobalFilterDTO();
filterDTO.setClient(Arrays.asList(1, 3, 4, 5));
filterDTO.setSegment(Arrays.asList("NA", "ECA"));
filterDTO.setRegion(Arrays.asList("US", "UK", "Russia"));
filterDTO.setCountry(Arrays.asList("USA", "GBR", "RUS"));

// Build a query with the filters
String baseQuery = "SELECT client_name FROM client_table";
String finalQuery = queryBuilderService.buildQuery(baseQuery, filterDTO);

// Use the query with JdbcTemplate
List<Map<String, Object>> results = jdbcTemplate.queryForList(finalQuery);
=========================================================
Map<String, GenericFilterService.FilterCondition<?>> filterMap = new HashMap<>();
filterMap.put("mdm_gems_ult_parent_id", 
    new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.IN, Arrays.asList(1, 3, 4, 5)));
filterMap.put("client_name", 
    new GenericFilterService.FilterCondition<>(GenericFilterService.FilterType.LIKE, "%Company%"));

String whereClause = genericFilterService.buildWhereClause(filterMap);
String finalQuery = "SELECT client_name FROM client_table WHERE " + whereClause;

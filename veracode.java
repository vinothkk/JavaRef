@Override
public List<InquiryByOwnerSeriesDTO> getInquiriesByOwner(GlobalFilterDTO globalFilterDTO) throws SQLException {
    logger.info("@Repository : getInquiriesByOwner()");
    
    StringBuilder sqlBuilder = new StringBuilder();
    Map<String, Object> paramMap = new HashMap<>();
    
    // Build the base query with placeholders instead of direct concatenation
    sqlBuilder.append(GoldmtConstants.IMT_INQUIRIES_BY_OWNER_BASE_QUERY);
    
    // Add global filters with parameterized values
    String globalFilterQuery = applyGlobalFilterLogicWithParams(globalFilterDTO, paramMap);
    if (globalFilterQuery != null && !globalFilterQuery.isEmpty()) {
        sqlBuilder.append(globalFilterQuery);
    }
    
    // Add detail filters with parameterized values
    String detailFilterQuery = applyInquiryDetailFilterLogicWithParams(globalFilterDTO, paramMap);
    if (detailFilterQuery != null && !detailFilterQuery.isEmpty()) {
        sqlBuilder.append(detailFilterQuery);
    }
    
    // Add the sub-query
    sqlBuilder.append(GoldmtConstants.IMT_INQUIRIES_BY_OWNER_SUB_QU);
    
    // Apply schema replacement
    String finalQuery = Utility.SqlWithSchema(sqlBuilder.toString());
    
    // Use named parameter JDBC template for parameterized queries
    NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    return namedParameterJdbcTemplate.query(finalQuery, paramMap, new InquiriesByOwnerRowMapper());
}

// Modified versions of your filter methods to use parameterized queries
private String applyGlobalFilterLogicWithParams(GlobalFilterDTO globalFilterDTO, Map<String, Object> paramMap) {
    // Modified version of applyGlobalFilterLogic that uses named parameters
    // Example: Instead of "column = 'value'", use "column = :paramName"
    // and add to paramMap.put("paramName", "value");
    
    // Return the SQL fragment with named parameters
    // Implementation depends on your existing applyGlobalFilterLogic method
    
    // This is just a placeholder - you'll need to adapt your actual method
    return ""; // return the SQL with named parameters
}

private String applyInquiryDetailFilterLogicWithParams(GlobalFilterDTO globalFilterDTO, Map<String, Object> paramMap) {
    // Similar to above, but for detail filters
    // Implementation depends on your existing applyInquiryDetailFilterLogic method
    
    // This is just a placeholder - you'll need to adapt your actual method
    return ""; // return the SQL with named parameters
}

private String applyGlobalFilterLogicWithParams(GlobalFilterDTO globalFilterDTO, Map<String, Object> paramMap) {
    StringBuilder filterQuery = new StringBuilder();
    
    if (globalFilterDTO != null) {
        // Handle client filter
        if (globalFilterDTO.getClient() != null && !globalFilterDTO.getClient().isEmpty()) {
            filterQuery.append(" and client_id in (:clientIds) ");
            paramMap.put("clientIds", globalFilterDTO.getClient());
        }
        
        // Handle region filter
        if (globalFilterDTO.getRegion() != null && !globalFilterDTO.getRegion().isEmpty()) {
            filterQuery.append(" and region in (:regions) ");
            paramMap.put("regions", globalFilterDTO.getRegion());
        }
        
        // Handle segment filter
        if (globalFilterDTO.getSegment() != null && !globalFilterDTO.getSegment().isEmpty()) {
            filterQuery.append(" and client_segment in (:segments) ");
            paramMap.put("segments", globalFilterDTO.getSegment());
        }
        
        // Handle country name filter
        if (globalFilterDTO.getCountryName() != null && !globalFilterDTO.getCountryName().isEmpty()) {
            filterQuery.append(" and country_name in (:countryNames) ");
            paramMap.put("countryNames", globalFilterDTO.getCountryName());
        }
    }
    
    return filterQuery.toString();
}


private String applyGlobalFilterLogicWithParams(GlobalFilterDTO globalFilterDTO, Map<String, Object> paramMap) {
    StringBuilder filterQuery = new StringBuilder();
    
    if (globalFilterDTO != null) {
        // Handle client filter
        if (globalFilterDTO.getClient() != null && !globalFilterDTO.getClient().isEmpty()) {
            filterQuery.append(" and client_id in (:clientIds) ");
            paramMap.put("clientIds", globalFilterDTO.getClient());
        }
        
        // Handle region filter
        if (globalFilterDTO.getRegion() != null && !globalFilterDTO.getRegion().isEmpty()) {
            filterQuery.append(" and region in (:regions) ");
            paramMap.put("regions", globalFilterDTO.getRegion());
        }
        
        // Handle segment filter
        if (globalFilterDTO.getSegment() != null && !globalFilterDTO.getSegment().isEmpty()) {
            filterQuery.append(" and client_segment in (:segments) ");
            paramMap.put("segments", globalFilterDTO.getSegment());
        }
        
        // Handle country name filter
        if (globalFilterDTO.getCountryName() != null && !globalFilterDTO.getCountryName().isEmpty()) {
            filterQuery.append(" and country_name in (:countryNames) ");
            paramMap.put("countryNames", globalFilterDTO.getCountryName());
        }
        
        // Handle date range filter
        if (globalFilterDTO.getCreatedOn() != null && !globalFilterDTO.getCreatedOn().isEmpty() 
                && globalFilterDTO.getCreatedOn().size() >= 2) {
            String fromDate = globalFilterDTO.getCreatedOn().get(1);
            String toDate = globalFilterDTO.getCreatedOn().get(0);
            
            if (fromDate != null && toDate != null) {
                filterQuery.append(" and to_date(to_timestamp(CREATED_DATETIME,'MM/d/yyyy h:m a')) >= to_date(to_timestamp(:fromDate,'MM/d/yyyy h:m a')) ");
                filterQuery.append(" and to_date(to_timestamp(CREATED_DATETIME,'MM/d/yyyy h:m a')) <= to_date(to_timestamp(:toDate,'MM/d/yyyy h:m a')) ");
                
                paramMap.put("fromDate", fromDate);
                paramMap.put("toDate", toDate);
            }
        }
    }
    
    return filterQuery.toString();
}

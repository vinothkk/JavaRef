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

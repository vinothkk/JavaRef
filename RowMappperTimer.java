import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.JdbcUtils; // For closing ResultSet if needed, though JdbcTemplate handles it

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit; // For cleaner duration output

// ... other imports

// Assume ProductServicesDTO.RowMapper exists and is accessible
final ProductServicesDTO.RowMapper rowMapper = new ProductServicesDTO.RowMapper();

StopWatch overallStopWatch = new StopWatch();
StopWatch mappingStopWatch = new StopWatch(); // To time only the mapping part
long queryExecutionTimeEstimateNanos = 0; // Will hold time outside mapping loop

overallStopWatch.start("JDBC Query Default with Extractor");

List<ProductServicesDTO> resultList = jdbcTemplate.query(
    Utility.SQLWithSchema(ProductAndServicesConstants.PRODUCT_SERVICES_UPDATED, getSchemaName()),
    new ResultSetExtractor<List<ProductServicesDTO>>() {
        @Override
        public List<ProductServicesDTO> extractData(ResultSet rs) throws SQLException, DataAccessException {
            List<ProductServicesDTO> dtos = new ArrayList<>();
            int rowNum = 0;
            // --- Time spent BEFORE this loop is mostly query execution + network latency ---

            mappingStopWatch.start("RowMapping"); // Start timing just before the loop
            try {
                while (rs.next()) {
                    // Inside the loop: call the original row mapper
                    dtos.add(rowMapper.mapRow(rs, rowNum++));
                }
            } finally {
                 // Ensure mapping timer stops even if mapRow throws an exception
                 if (mappingStopWatch.isRunning()) {
                    mappingStopWatch.stop();
                 }
            }
            // --- Time spent AFTER this loop (but before returning) is minimal ---
            return dtos;
        }
    }
);

overallStopWatch.stop();

// --- Calculate Timings ---

long totalNanos = overallStopWatch.getTotalTimeNanos();
long mappingNanos = mappingStopWatch.getTotalTimeNanos();
// Estimate: Time not spent mapping is query execution + network fetch + driver overhead
long queryAndNetworkNanos = totalNanos - mappingNanos;

System.out.println("--- Execution Timings ---");
System.out.println("Total Operation Time : " + TimeUnit.NANOSECONDS.toMillis(totalNanos) + " ms");
System.out.println("Row Mapping Time     : " + TimeUnit.NANOSECONDS.toMillis(mappingNanos) + " ms");
System.out.println("Est. Query + Network : " + TimeUnit.NANOSECONDS.toMillis(queryAndNetworkNanos) + " ms");
System.out.println("Total Rows Mapped    : " + (resultList != null ? resultList.size() : 0));
if (resultList != null && !resultList.isEmpty() && mappingNanos > 0) {
    System.out.println("Avg Time Per Row Map : " + String.format("%.4f", (double)mappingNanos / resultList.size() / 1_000_000.0) + " ms");
}


// return resultList; // Use the resultList obtained
return defa; // Keep your original return if needed, but use resultList above

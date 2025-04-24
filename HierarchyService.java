import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
// Assuming YourRepository fetches the data
// import com.yourcompany.repository.YourRepository; 

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HierarchyService {

    private static final Logger log = LoggerFactory.getLogger(HierarchyService.class);

    // Inject your repository (replace YourRepository with your actual class)
    // @Autowired
    // private YourRepository repository;

    public List<ParentData> buildHierarchy(List<RepositoryResponseItem> flatData) {
        if (flatData == null || flatData.isEmpty()) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        log.info("Starting hierarchy build for {} items.", flatData.size());

        // --- Step 1: Group by Parent ID ---
        // Creates a Map where Key = parentId (String), Value = List of items for that parent
        Map<String, List<RepositoryResponseItem>> itemsByParent = flatData.stream()
                // Consider parallelStream() for very large datasets, but benchmark first!
                // .parallelStream() 
                .collect(Collectors.groupingBy(RepositoryResponseItem::parentId));

        // --- Step 2: Process each Parent Group ---
        List<ParentData> result = new ArrayList<>();
        itemsByParent.forEach((parentIdStr, parentItems) -> {

            // Get representative item for parent details (assuming they are consistent)
            RepositoryResponseItem representativeParentItem = parentItems.get(0);

            // --- Step 2a: Group Parent's Items by Client ID ---
            // Creates a Map where Key = clientId (String), Value = List of items for that client (under this parent)
            Map<String, List<RepositoryResponseItem>> itemsByClient = parentItems.stream()
                    .collect(Collectors.groupingBy(RepositoryResponseItem::mdmClientGemsId));

            List<ClientData> clientSubRows = new ArrayList<>();
            itemsByClient.forEach((clientId, clientItems) -> {

                // Get representative item for client details
                RepositoryResponseItem representativeClientItem = clientItems.get(0);

                // --- Step 2b: Group Client's Items by Customer ID ---
                // Creates a Map where Key = customerId (String), Value = List of items for that customer (often size 1)
                Map<String, List<RepositoryResponseItem>> itemsByCustomer = clientItems.stream()
                        .collect(Collectors.groupingBy(RepositoryResponseItem::mdmCustGemsId));

                List<CustomerData> customerSubRows = new ArrayList<>();
                itemsByCustomer.forEach((customerId, customerItems) -> {
                    // Usually only one item per customer in this structure
                    RepositoryResponseItem customerItem = customerItems.get(0);

                    // --- Step 2c: Create CustomerData ---
                    CustomerData customerData = new CustomerData(
                            customerId,
                            clientId, // Parent is the Client ID
                            customerItem.customerName(),
                            "Customer",
                            representativeParentItem.segment(), // Inherit from parent
                            representativeParentItem.region(),  // Inherit from parent
                            customerItem.aPlatformCustomerCount(),
                            customerItem.alphaServicesCustomerCount(),
                            customerItem.backOfficeCustomerCount(),
                            customerItem.custodyCustomerCount(),
                            customerItem.digitalCustomerCount(),
                            customerItem.globalMarketsCustomerCount(),
                            customerItem.middleOfficeCustomerCount(), // check name consistency
                            customerItem.ssgaCustomerCount(),
                            customerItem.treasuryCustomerCount(),
                            Collections.emptyList() // Empty detailData as requested
                    );
                    customerSubRows.add(customerData);
                }); // End Customer processing

                // --- Step 2d: Create ClientData ---
                 long parentIdLong;
                 try {
                     parentIdLong = Long.parseLong(parentIdStr);
                 } catch (NumberFormatException e) {
                     log.error("Invalid parent_id format: {}. Skipping client hierarchy for this parent.", parentIdStr, e);
                     // Decide how to handle - skip this client/parent, use a default, etc.
                     // Here we skip adding this client by returning from the lambda for this client
                     return; 
                 }

                ClientData clientData = new ClientData(
                        clientId,
                        parentIdLong, // Parent is the Parent ID (parsed)
                        representativeClientItem.clientName(),
                        "Client",
                        representativeParentItem.segment(), // Inherit from parent
                        representativeParentItem.region(),  // Inherit from parent
                        representativeClientItem.aPlatformClientCount(), // Add null checks if needed
                        representativeClientItem.alphaServicesClientCount(),
                        representativeClientItem.backOfficeClientCount(),
                        representativeClientItem.custodyClientCount(),
                        representativeClientItem.digitalClientCount(),
                        representativeClientItem.globalMarketsClientCount(),
                        representativeClientItem.middleOfficeClientCount(),
                        representativeClientItem.ssgaClientCount(),
                        representativeClientItem.treasuryClientCount(),
                        customerSubRows // Attach the processed customers
                );
                clientSubRows.add(clientData);

            }); // End Client processing


            // --- Step 2e: Create ParentData ---
             long parentIdLong;
             try {
                 parentIdLong = Long.parseLong(parentIdStr);
             } catch (NumberFormatException e) {
                 log.error("Invalid parent_id format: {}. Skipping parent hierarchy.", parentIdStr, e);
                  // Decide how to handle - skip this parent, use a default, etc.
                  // Here we skip adding this parent by returning from the lambda for this parent
                 return; 
             }

            ParentData parentData = new ParentData(
                    parentIdLong,
                    representativeParentItem.parentName(),
                    "Parent",
                    representativeParentItem.segment(),
                    representativeParentItem.region(),
                    representativeParentItem.aPlatformParentCount(),
                    representativeParentItem.alphaServicesParentCount(),
                    representativeParentItem.backOfficeParentCount(),
                    representativeParentItem.custodyParentCount(),
                    representativeParentItem.digitalParentCount(),
                    representativeParentItem.globalMarketsParentCount(),
                    representativeParentItem.middleOfficeParentCount(), // Check name consistency
                    representativeParentItem.ssgaParentCount(),
                    representativeParentItem.treasuryParentCount(),
                    clientSubRows // Attach the processed clients
            );
            result.add(parentData);

        }); // End Parent processing

        long endTime = System.currentTimeMillis();
        log.info("Hierarchy build completed in {} ms. Result size: {}", (endTime - startTime), result.size());

        return result;
    }

    // --- Example of how you might call this from a controller ---
    // Assume you have a method in your repository getFlatData()
    // public List<ParentData> getHierarchicalData() {
    //     List<RepositoryResponseItem> rawData = repository.getFlatData(); // Fetch data
    //     return buildHierarchy(rawData); // Transform data
    // }

}









/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/hierarchy")
public class HierarchyController {

    @Autowired
    private HierarchyService hierarchyService;

    @Autowired // Assuming you have a repository bean
    private YourRepository repository; // Replace with your actual repository

    @GetMapping
    public List<ParentData> getHierarchy() {
        // In a real app, you might pass parameters to the repo
        List<RepositoryResponseItem> flatData = repository.getFlatData(); 
        return hierarchyService.buildHierarchy(flatData);
    }
}

*/

@Test
void testGetProductServicesOptimization_Success() throws Exception {
    // Arrange
    List<CustomerData> customers = List.of(
        new CustomerData("cust1", "client1", "Customer 1", "Customer", "Segment A", "Region A",
            "1", "2", "3", "4", "5", "6", "7", "8", List.of())
    );

    List<ClientData> clients = List.of(
        new ClientData("client1", 1L, "Client 1", "Client", "Segment A", "Region A",
            "1", "2", "3", "4", "5", "6", "7", "8", customers)
    );

    ParentData parentData = new ParentData(
        1L,
        "Parent 1",
        "Parent",
        "Segment A",
        "Region A",
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        clients
    );

    List<ParentData> mockDataList = List.of(parentData);

    ResponseData<List<ParentData>> responseData = new ResponseData<>();
    responseData.setData(mockDataList);
    responseData.setSuccess(true);
    responseData.setMessage("Success");

    ResponseEntity<ResponseData<List<ParentData>>> responseEntity =
            new ResponseEntity<>(responseData, HttpStatus.OK);

    Mockito.when(productServices.getProductServicesOptimization(globalFilterDTO))
           .thenReturn(responseEntity);

    // Act
    ResponseEntity<ResponseData<List<ParentData>>> response =
            productServicesController.getProductServicesOptimization(globalFilterDTO);

    // Assert
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals("Success", response.getBody().getMessage());
    Assertions.assertEquals(1, response.getBody().getData().size());
    Assertions.assertEquals("Parent 1", response.getBody().getData().get(0).name());
}

<!-- For JUnit 5, Mockito, and Spring Boot test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

@ExtendWith(MockitoExtension.class)
class ProductServicesControllerTest {

    @InjectMocks
    private ProductServicesController productServicesController;

    @Mock
    private ProductServices productServices;

    private GlobalFilterDTO globalFilterDTO;
    private List<ParentDataRecordDTO> mockDataList;
    private ResponseData<List<ParentDataRecordDTO>> responseData;

    @BeforeEach
    void setUp() {
        globalFilterDTO = new GlobalFilterDTO(); // initialize with test data if needed
        mockDataList = new ArrayList<>();
        mockDataList.add(new ParentDataRecordDTO()); // sample DTO
        responseData = new ResponseData<>();
        responseData.setData(mockDataList);
        responseData.setSuccess(true);
        responseData.setMessage("Success");
    }

    @Test
    void testGetProductServicesOptimization_Success() throws Exception {
        // Arrange
        ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> responseEntity =
                new ResponseEntity<>(responseData, HttpStatus.OK);
        Mockito.when(productServices.getProductServicesOptimization(globalFilterDTO))
                .thenReturn(responseEntity);

        // Act
        ResponseEntity<ResponseData<List<ParentDataRecordDTO>>> response =
                productServicesController.getProductServicesOptimization(globalFilterDTO);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("Success", response.getBody().getMessage());
        Assertions.assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void testGetProductServicesOptimization_ServiceThrowsException() throws Exception {
        // Arrange
        Mockito.when(productServices.getProductServicesOptimization(globalFilterDTO))
                .thenThrow(new RuntimeException("Service failure"));

        // Act & Assert
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
            productServicesController.getProductServicesOptimization(globalFilterDTO);
        });

        Assertions.assertEquals("Service failure", exception.getMessage());
    }
}

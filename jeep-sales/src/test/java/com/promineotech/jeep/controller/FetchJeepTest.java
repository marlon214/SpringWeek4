package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import com.promineotech.jeep.Constants;
import com.promineotech.jeep.controller.support.FetchJeepTestSupport;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesServices;



class FetchJeepTest{
  
  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(scripts = { 
      "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, 
      config = @SqlConfig(encoding = "utf-8"))
  class TestsThatDoNotPolluteTheApplications extends FetchJeepTestSupport{
    //JUnit will not pick it up if it is public
    
    @Test
    void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {
      // Given: a valid model, trim and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Sport";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
      
      //When a connection is made to the URI
      ResponseEntity<List<Jeep>> response= 
          getRestTemplate().exchange(uri, HttpMethod.GET, null, 
              new ParameterizedTypeReference<>() {});
      
      // Then a 200 HTTP Status code is return 
      
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      
      //actual list is the same as expected list
      List<Jeep> actual = response.getBody();
      List<Jeep> expected = buildExpected();
      
      
    //  actual.forEach(jeep -> jeep.setModelId(null));
      assertThat(actual).isEqualTo(expected);

    }
    
    @Test
    void testThatAnErrorMessageIsReturnedWhenAnUnknownTrimIsSuppplied() {
      //Similar to our previous test method
      // Given: a valid model, trim and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Unknown Value";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
      
      //When a connection is made to the URI
      //Response will not be a jeep list
      ResponseEntity<Map<String, Object>> response= 
          getRestTemplate().exchange(uri, HttpMethod.GET, null, 
              new ParameterizedTypeReference<>() {});
      
      // Then a 404 HTTP Status code is return 
      
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      
      //an error message is returned
      // This is a map now because that is the type we return to or response entity
      Map<String, Object> error = response.getBody();
      
      assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
      
    }
    
    @ParameterizedTest
    @MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
    void testThatAnErrorMessageIsReturnedWhenAnInvalidValueIsSuppplied(
        String model, String trim, String reason) {

      // Given: a valid model, trim and URI
      
      //These are removed because we are now setting them as parameter
      // JeepModel model = JeepModel.WRANGLER;
      //String trim = "Invalid Value";
      
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
      
      //When a connection is made to the URI
      //Response will not be a jeep list
      ResponseEntity<Map<String, Object>> response= 
          getRestTemplate().exchange(uri, HttpMethod.GET, null, 
              new ParameterizedTypeReference<>() {});
      
      // Then a 400 HTTP Status code is return 
      
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      
      //an error message is returned
      // This is a map now because that is the type we return to or response entity
      Map<String, Object> error = response.getBody();
      
      //the value for the test is negligible 
      //its important to set these keys in our error message
      assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);

  }
  
  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(scripts = { 
      "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, 
      config = @SqlConfig(encoding = "utf-8"))
  
  class TestsThattPolluteTheApplications extends FetchJeepTestSupport{
  @MockBean
  private JeepSalesServices jeepSalesService;
  
  @Test
  void testThatAnUnplannedErrorResultsInA500Status() {
    //Similar to our previous test method
    // Given: a valid model, trim and URI
    JeepModel model = JeepModel.WRANGLER;
    String trim = "Invalid";
    String uri = 
        String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
    
    doThrow(new RuntimeException("Ouch!")).when(jeepSalesService).fetchJeeps(model, trim);
    
    //When a connection is made to the URI
    //Response will not be a jeep list
    ResponseEntity<Map<String, Object>> response= 
        getRestTemplate().exchange(uri, HttpMethod.GET, null, 
            new ParameterizedTypeReference<>() {});
    
 
    // Then a 500 HTTP Status code is return 
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    
    //an error message is returned
    // This is a map now because that is the type we return to or response entity
    Map<String, Object> error = response.getBody();
    
    assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);
    
  }
  }

//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//    
//    @Test
//    void testDB(){
//      int numrows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "customers");
//      System.out.println("num" + numrows);
//    }
  
  
 
}

  static Stream <Arguments> parametersForInvalidInput(){
    //// @formatter:off
    //takes an array and returns a string
    return Stream.of(
        arguments("WRANGLER", "%#@%^#$", "Trim contains non-alpha-numeric chars"),
        arguments("WRANGLER", "C".repeat(Constants.TRIM_MAX_LENGTH + 1), "Trim length too long"),
        arguments("INVALID", "Sport", "Model is not enum value")
        );
    //import static org.junit.jupiter.params.provider.Arguments.arguments;
// @formatter:on   
  }
}
  

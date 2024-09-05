package io.github.isagroup.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.yaml.snakeyaml.Yaml;

import io.github.isagroup.exceptions.PricingParsingException;
import io.github.isagroup.models.Plan;
import io.github.isagroup.models.Version;
import io.github.isagroup.models.PricingManager;
import io.github.isagroup.services.parsing.PricingManagerParser;
import io.github.isagroup.services.yaml.YamlUtils;

public class PricingManagerParserTest {

  @Test
  void givenPetclinicShouldGetPricingManager() {

    PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml("pricing/petclinic.yml");

    assertTrue(pricingManager.getPlans().get("BASIC") instanceof Plan,
        "Should be an instance of PricingManager");
    assertEquals(false,
        pricingManager.getPlans().get("BASIC").getFeatures().get("haveCalendar")
            .getDefaultValue(),
        "The deafult value of the haveCalendar feature should be false");
    assertEquals(null, pricingManager.getPlans().get("BASIC").getFeatures().get("maxPets").getValue(),
        "The value of the maxPets should be null");

  }

  @Test
  void givenNoVersionShouldDefaultToVersionOneDotZero() {

    Yaml yaml = new Yaml();

    String file = """
        saasName: Pricing Without Version Should Default to 1.0
        currency: EUR
        hasAnnualPayment: false
        day: 31
        month: 8
        year: 2024
        features:
          foo:
            type: DOMAIN
            valueType: TEXT
            defaultValue: baz
        plans:
          BASIC:
            description: Basic plan
            monthlyPrice: 0.0
            annualPrice: 0.0
            unit: user/month
            features: null
            usageLimits: null
        """;

    Map<String, Object> configFile = yaml.load(file);

    PricingManager pricingManager = PricingManagerParser.parseMapToPricingManager(configFile);

    assertEquals(pricingManager.getVersion(), new Version(1, 0));

  }

  @Test
  void givenOneDotZeroVersionInFileShouldBeOk() {

    Yaml yaml = new Yaml();

    String file = """
        version: "1.0"
        saasName: Pricing Without Version Should Default to 1.0
        currency: EUR
        hasAnnualPayment: false
        day: 31
        month: 8
        year: 2024
        features:
          foo:
            type: DOMAIN
            valueType: TEXT
            defaultValue: baz
        plans:
          BASIC:
            description: Basic plan
            monthlyPrice: 0.0
            annualPrice: 0.0
            unit: user/month
            features: null
            usageLimits: null
        """;

    Map<String, Object> configFile = yaml.load(file);

    PricingManager pricingManager = PricingManagerParser.parseMapToPricingManager(configFile);

    assertEquals(pricingManager.getVersion(), new Version(1, 0));

  }

  @Test
  // Cammbiar
  void givenPricingVersionOneDotZeroAsFloatShouldThrow() {

    Yaml yaml = new Yaml();

    String file = """
        version: 1.0
        saasName: Pricing Without Version Should Default to 1.0
        currency: EUR
        hasAnnualPayment: false
        day: 31
        month: 8
        year: 2024
        features:
          foo:
            type: DOMAIN
            valueType: TEXT
            defaultValue: baz
        plans:
          BASIC:
            description: Basic plan
            monthlyPrice: 0.0
            annualPrice: 0.0
            unit: user/month
            features: null
            usageLimits: null
        """;

    Map<String, Object> configFile = yaml.load(file);

    assertThrows(PricingParsingException.class,
        () -> PricingManagerParser.parseMapToPricingManager(configFile));

  }

  @Test
  // Cambiar date a localdate
  void givenOneDotOneShouldDectectNewSyntax() {

    Yaml yaml = new Yaml();

    String file = """
        version: "1.1"
        saasName: Version and old syntax missmatch
        currency: EUR
        hasAnnualPayment: false
        createdAt: "2024-08-30"
        starts: 2024-01-01 12:00:00
        ends: 2025-01-01 12:00:00
        features:
          foo:
            type: DOMAIN
            valueType: TEXT
            defaultValue: baz
        plans:
          BASIC:
            description: Basic plan
            monthlyPrice: 0.0
            annualPrice: 0.0
            unit: user/month
            features: null
            usageLimits: null
        """;

    Map<String, Object> configFile = yaml.load(file);

    PricingManager pricingManager = PricingManagerParser.parseMapToPricingManager(configFile);

    assertEquals(pricingManager.getCreatedAt(), LocalDate.of(2024, 8, 30));
    // 1704110400000 milliseconds => 2024-01-01 12:00:00
    assertEquals(pricingManager.getStarts(), new Date(1704110400000L));
    // 1735732800000 milliseconds => 2025-01-01 12:00:00
    assertEquals(pricingManager.getEnds(), new Date(1735732800000L));
  }

  @ParameterizedTest
  @CsvSource({
      "null-saasName,SaasName was not defined",
      "boolean-in-saasName,'saasName' has to be a string",
      "null-day,Day of plan was not defined",
      "boolean-in-day,'day' is expected to be an integer",
      "null-month,Month of plan was not defined",
      "boolean-in-month,'month' is expected to be an integer",
      "null-year,Year of plan was not defined",
      "boolean-in-year,'year' is expected to be an integer",
      "invalid-date-1.0,'Cannot convert 2024-13-31 to a LocalDate. Check that day, month and year are valid.'",
      "null-currency,Currency was not defined",
      "boolean-in-currency,'currency' has to be a string",
      "null-features,'features' is mandatory. It should be a map of features with their correspoding attributes.",
      "string-in-features,The features are not defined correctly. It should be a map of features and their options.",
      "boolean-in-features,The features are not defined correctly. It should be a map of features and their options.",
      "integer-in-features,The features are not defined correctly. It should be a map of features and their options.",
      "float-in-features,The features are not defined correctly. It should be a map of features and their options.",
      "list-in-features,The features are not defined correctly. It should be a map of features and their options.",
      "null-plans-and-addons,The pricing manager does not have any plans or add ons",
      "boolean-in-plans,The plans are not defined correctly. It should be a map of plans and their options",
      "string-in-plans,The plans are not defined correctly. It should be a map of plans and their options",
      "integer-in-plans,The plans are not defined correctly. It should be a map of plans and their options",
      "float-in-plans,The plans are not defined correctly. It should be a map of plans and their options",
      "list-in-plans,The plans are not defined correctly. It should be a map of plans and their options",
      "invalid-version,version 'This is an invalid version :(' is invalid",
      "version-1.1-mix-version-1.0,'You have specified version 1.1 of the config but old configuration fields were encountered from version 1.0 (day, month, year). Please use createdAt and remove day, month and year or remove the version field.'",
      "null-createdAt-version-1.1,'createdAt' is mandatory. Check your config file.",
      "boolean-createdAt-version-1.1,'createdAt is not a string or a date, change that field.'",
      "invalid-string-createdAt-version-1.1,date Invalid date format :( is invalid. Use the following format to specify a date yyyy-MM-dd.",
      "invalid-timestamp-starts,starts is expected to be a timestamp. Check your config file.",
      "invalid-timestamp-ends,ends is expected to be a timestamp. Check your config file."
  })
  void givenIncorrectTypeInSaaSNameShouldThrow(String input, String expectedErrorMessage) {

    Yaml yaml = new Yaml();
    String path = String.format("src/test/resources/parsing/%s.yml", input);
    try {
      Map<String, Object> configFile = yaml
          .load(new FileInputStream(path));
      PricingManagerParser.parseMapToPricingManager(configFile);
      fail();
    } catch (FileNotFoundException e) {
      fail(String.format("The file with location '%s' was not found", path));
    } catch (PricingParsingException e) {

      assertEquals(expectedErrorMessage, e.getMessage());

    }

  }

}

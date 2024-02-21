package io.github.isagroup;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.github.isagroup.exceptions.FeatureNotFoundException;
import io.github.isagroup.exceptions.InvalidDefaultValueException;
import io.github.isagroup.models.AddOn;
import io.github.isagroup.models.Feature;
import io.github.isagroup.models.Plan;
import io.github.isagroup.models.PricingManager;
import io.github.isagroup.models.UsageLimit;
import io.github.isagroup.models.ValueType;
import io.github.isagroup.models.featuretypes.Automation;
import io.github.isagroup.models.featuretypes.Domain;
import io.github.isagroup.models.usagelimittypes.NonRenewable;
import io.github.isagroup.models.usagelimittypes.Renewable;
import io.github.isagroup.services.yaml.YamlUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PricingServiceTests {

    private static final String JWT_SECRET_TEST = "secret";
    private static final Integer JWT_EXPIRATION_TEST = 86400;
    private static final String JWT_SUBJECT_TEST = "admin1";
    private static final String PETCLINIC_CONFIG_PATH = "pricing/petclinic.yml";

    private static final String TEMPORAL_CONFIG_PATH = "yaml-testing/temp.yml";
    private boolean removeTempFile = true;

    private static final String TEST_PLAN = "BASIC";
    private static final String TEST_NEW_PLAN = "NEW_PLAN";
    private static Plan newPlan = new Plan();
    private static UsageLimit newUsageLimit = new Renewable();
    private static AddOn newAddOn = new AddOn();

    private static final String TEST_BOOLEAN_FEATURE = "haveCalendar";
    private static final String TEST_NUMERIC_FEATURE = "maxPets";
    private static final String TEST_TEXT_FEATURE = "supportPriority";
    private static final String TEST_NEW_ADDON_FEATURE = "haveVetSelection";
    private static final String NEW_FEATURE_NAME = "newFeature";
    private static final Integer NEW_FEATURE_TEST_VALUE = 3;
    private static final String NEW_FEATURE_TEST_EXPRESSION = "userContext['pets'] > 1";
    private static final PricingManager ORIGINAL_PRICING_MANAGER = YamlUtils
            .retrieveManagerFromYaml(PETCLINIC_CONFIG_PATH);

    private PricingService pricingService;

    private PricingContextTestImpl pricingContextTestImpl;

    @BeforeAll
    static void setUp() {

        // Creation of new plan

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(PETCLINIC_CONFIG_PATH);

        newPlan.setName(TEST_NEW_PLAN);
        newPlan.setDescription("New plan description");
        newPlan.setMonthlyPrice(2.0);
        newPlan.setAnnualPrice(1.0);
        newPlan.setUnit("clinic/month");

        Map<String, Feature> features = pricingManager.getPlans().get(TEST_PLAN).getFeatures();

        Feature newBooleanFeature = features.get(TEST_BOOLEAN_FEATURE);
        Feature newNumericFeature = features.get(TEST_NUMERIC_FEATURE);
        Feature newTextFeature = features.get(TEST_TEXT_FEATURE);

        newBooleanFeature.setValue(true);
        newNumericFeature.setValue(6);
        newTextFeature.setValue("HIGH");

        features.put(TEST_BOOLEAN_FEATURE, newBooleanFeature);
        features.put(TEST_NUMERIC_FEATURE, newNumericFeature);
        features.put(TEST_TEXT_FEATURE, newTextFeature);

        newPlan.setFeatures(features);

        // Creation of new add-on

        newAddOn.setName("newAddOn");
        newAddOn.setMonthlyPrice(5.0);
        newAddOn.setAnnualPrice(4.0);
        newAddOn.setUnit("owner/month");

        List<String> availableFor = new ArrayList<>();
        availableFor.add(pricingManager.getPlans().get(TEST_PLAN).getName());
        newAddOn.setAvailableFor(availableFor);

        Feature newAddOnFeature = pricingManager.getFeatures().get(TEST_NEW_ADDON_FEATURE);
        Map<String, Feature> addOnFeatures = new HashMap<>();
        addOnFeatures.put(TEST_NEW_ADDON_FEATURE, newAddOnFeature);
        newAddOn.setFeatures(addOnFeatures);

        UsageLimit addOnUsageLimitExtension = new NonRenewable();
        addOnUsageLimitExtension.setName("maxPets");
        addOnUsageLimitExtension.setDescription("Max pets extension description");
        addOnUsageLimitExtension.setValueType(ValueType.NUMERIC);
        addOnUsageLimitExtension.setDefaultValue(5);
        addOnUsageLimitExtension.setUnit("pet");
        addOnUsageLimitExtension.getLinkedFeatures().add(TEST_NUMERIC_FEATURE);
        Map<String, UsageLimit> usageLimitsExtensions = new HashMap<>();
        usageLimitsExtensions.put(addOnUsageLimitExtension.getName(), addOnUsageLimitExtension);
        newAddOn.setUsageLimitsExtensions(usageLimitsExtensions);
    }

    @BeforeEach
    public void init() {

        // Read petclinic
        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(PETCLINIC_CONFIG_PATH);
        // Write temporal petclinic config file
        YamlUtils.writeYaml(pricingManager, TEMPORAL_CONFIG_PATH);

        Map<String, Object> userContext = new HashMap<>();
        userContext.put("username", JWT_SUBJECT_TEST);
        userContext.put("pets", 2);

        Map<String, Object> userAuthorities = new HashMap<>();
        userAuthorities.put("role", "admin");
        userAuthorities.put("username", JWT_SUBJECT_TEST);
        userAuthorities.put("password", "4dm1n");

        PricingContextTestImpl pricingContextTest = new PricingContextTestImpl();
        pricingContextTest.setConfigFilePath(TEMPORAL_CONFIG_PATH);
        pricingContextTest.setJwtExpiration(JWT_EXPIRATION_TEST);
        pricingContextTest.setJwtSecret(JWT_SECRET_TEST);
        pricingContextTest.setUserPlan(TEST_PLAN);
        pricingContextTest.setUserAuthorities(userAuthorities);
        pricingContextTest.setUserContext(userContext);

        // Reset of new usage limit

        newUsageLimit.setName("newUsageLimit");
        newUsageLimit.setDescription("New usage limit description");
        newUsageLimit.setValueType(ValueType.NUMERIC);
        newUsageLimit.setDefaultValue(10);
        newUsageLimit.setUnit("appointment");
        newUsageLimit.getLinkedFeatures().add(TEST_BOOLEAN_FEATURE);

        this.pricingContextTestImpl = pricingContextTest;
        this.pricingService = new PricingService(pricingContextTest);
        this.removeTempFile = true;
    }

    @AfterEach
    void after() {
        try {

            if (this.removeTempFile) {
                File file = new File("src/test/resources/yaml-testing/temp.yml");
                file.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------- PLAN RETRIEVAL ---------------------------

    @Test
    @Order(10)
    void shouldReturnPlanGivenPlanName() {

        Plan plan = pricingService.getPlanFromName("BASIC");

        assertInstanceOf(Plan.class, plan);

        assertEquals(0.0, plan.getAnnualPrice());
        assertEquals(0.0, plan.getMonthlyPrice());

    }

    @Test
    @Order(20)
    void shouldThrowExceptionGivenNonExistentPlan() {

        String nonExistentPlan = "nonExistentPlan";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pricingService.getPlanFromName(nonExistentPlan);
        });

        assertEquals("The plan " + nonExistentPlan + " does not exist in the current pricing configuration",
                exception.getMessage());

    }

    // // --------------------------- PLAN ADITION ---------------------------

    @Test
    @Order(30)
    void givenPlanShouldAddPlanToConfigFile() {

        pricingService.addPlanToConfiguration(newPlan);

        // FIXME
        // READS OLD VALUE
        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(TEMPORAL_CONFIG_PATH);

        assertTrue(pricingManager.getPlans().containsKey(TEST_NEW_PLAN),
                "Pricing config does not have NEW_PLAN");

    }

    @Test
    @Order(40)
    void givenDuplicatePlanNameShouldThrowExceptionWhenAddingPlan() {

        Plan newDuplicatePlan = newPlan;
        newDuplicatePlan.setName(TEST_PLAN);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pricingService.addPlanToConfiguration(newDuplicatePlan);
        });

        assertEquals("The plan " + TEST_PLAN + " already exists in the current pricing configuration",
                exception.getMessage());

    }

    // // --------------------------- PLAN REMOVAL ---------------------------

    @Test
    @Order(50)
    void givenExistingPlanNameShouldDeletePlanFromConfig() {

        String plan = "BASIC";

        pricingService.removePlanFromConfiguration(plan);

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(TEMPORAL_CONFIG_PATH);

        assertFalse(pricingManager.getPlans().containsKey(plan), "Basic plan was not removed");

    }

    @Test
    @Order(60)
    void givenNonExistingPlanNameShouldThrowWhenDeleting() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pricingService.removePlanFromConfiguration(TEST_NEW_PLAN);
        });

        assertEquals("There is no plan with the name " + TEST_NEW_PLAN + " in the current pricing configuration",
                exception.getMessage());

    }

    // // --------------------------- BOOLEAN EDITIONS ---------------------------

    @Test
    @Order(70)
    void givenExistingPlanNameAndFeatureShouldUpdateBooleanFeature() {

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        assertEquals(null, plan.getFeatures().get(TEST_BOOLEAN_FEATURE).getValue(),
                "haveCalendar from plan BASIC should be false");

        Feature feature = plan.getFeatures().get(TEST_BOOLEAN_FEATURE);

        feature.setValue(true);

        plan.getFeatures().put(TEST_BOOLEAN_FEATURE, feature);

        pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);

        pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertEquals(true,
                pricingManager.getPlans().get(TEST_PLAN).getFeatures().get(TEST_BOOLEAN_FEATURE).getValue(),
                TEST_BOOLEAN_FEATURE + " from plan BASIC should be true");

    }

    @Test
    @Order(80)
    void givenExistingPlanAndBooleanFeatureShouldThrowAssigningNumeric() {

        Integer newValue = 3;

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        assertEquals(null, plan.getFeatures().get(TEST_BOOLEAN_FEATURE).getValue(),
                TEST_BOOLEAN_FEATURE + " from plan BASIC should be null");

        assertEquals(ValueType.BOOLEAN, plan.getFeatures().get(TEST_BOOLEAN_FEATURE).getValueType(),
                TEST_BOOLEAN_FEATURE + "from plan BASIC should have BOOLEAN as its value type");

        Feature feature = plan.getFeatures().get(TEST_BOOLEAN_FEATURE);

        feature.setValue(newValue);

        plan.getFeatures().put(TEST_BOOLEAN_FEATURE, feature);

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);
        });
    }

    // --------------------------- NUMERIC EDITIONS ---------------------------

    @Test
    @Order(100)
    void givenExistingPlanAndNumericFeatureShouldUpdate() {

        Integer newValue = 6;

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        assertEquals(null, plan.getFeatures().get(TEST_NUMERIC_FEATURE).getValue(),
                TEST_NUMERIC_FEATURE + " value from plan BASIC should be null, as default value is used");

        Feature feature = plan.getFeatures().get(TEST_NUMERIC_FEATURE);

        feature.setValue(newValue);

        plan.getFeatures().put(TEST_NUMERIC_FEATURE, feature);

        pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);

        pricingManager = pricingContextTestImpl.getPricingManager();

        assertEquals(newValue,
                pricingManager.getPlans().get(TEST_PLAN).getFeatures().get(TEST_NUMERIC_FEATURE).getValue(),
                TEST_NUMERIC_FEATURE + " from plan BASIC should be " + newValue);
    }

    @Test
    @Order(110)
    void givenStringShouldThrowWhenUpdatingNumericFeature() {

        String newValue = "invalidValue";

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        assertEquals(null, plan.getFeatures().get(TEST_NUMERIC_FEATURE).getValue(),
                TEST_NUMERIC_FEATURE + " value from plan BASIC should be null, as default value is used");

        assertEquals(ValueType.NUMERIC, plan.getFeatures().get(TEST_NUMERIC_FEATURE).getValueType(),
                TEST_NUMERIC_FEATURE + " from plan BASIC should have NUMERIC as its value type");

        Feature feature = plan.getFeatures().get(TEST_NUMERIC_FEATURE);

        feature.setValue(newValue);

        plan.getFeatures().put(TEST_BOOLEAN_FEATURE, feature);

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);
        });
    }

    // --------------------------- TEXT EDITIONS ---------------------------

    @Test
    @Order(130)
    void givenStringShouldUpdateTextFeature() {

        String newValue = "HIGH";

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        assertEquals(null, plan.getFeatures().get(TEST_TEXT_FEATURE).getValue(),
                TEST_TEXT_FEATURE + " value from plan BASIC should be null, as default value is used");

        Feature feature = plan.getFeatures().get(TEST_TEXT_FEATURE);

        feature.setValue(newValue);

        plan.getFeatures().put(TEST_TEXT_FEATURE, feature);

        pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);

        pricingManager = pricingContextTestImpl.getPricingManager();

        assertEquals(newValue,
                pricingManager.getPlans().get(TEST_PLAN).getFeatures().get(TEST_TEXT_FEATURE).getValue(),
                TEST_TEXT_FEATURE + " from plan BASIC should be " + newValue);
    }

    @Test
    void givenNonExistentFeatureShouldThrowIllegalArgumentExceptionWhenUpdatingPlan() {

        assertThrows(IllegalArgumentException.class, () -> pricingService.updatePlanFromConfiguration("foo", newPlan));
    }

    @Test
    void givenNullPlanPreviousNameShouldTrowIllegalArgumentExceptionWhenUpdatingPlan() {
        // if plans map contains a null, string comparison will throw null pointer
        // exception
        assertThrows(IllegalArgumentException.class, () -> pricingService.updatePlanFromConfiguration(null, newPlan));
    }

    @Test
    void givenNewPlanNameShouldUpdateOnlyPlanName() {

        Map<String, Plan> oldPlans = YamlUtils.retrieveManagerFromYaml(TEMPORAL_CONFIG_PATH).getPlans();
        Plan basicPlan = oldPlans.get("BASIC");

        // FIXME with a copy constructor new Plan(Plan plan) is easy
        Plan updatedPlan = new Plan();
        updatedPlan.setName("foo");
        updatedPlan.setAnnualPrice(basicPlan.getAnnualPrice());
        updatedPlan.setMonthlyPrice(basicPlan.getMonthlyPrice());
        updatedPlan.setDescription(basicPlan.getDescription());
        updatedPlan.setUnit(basicPlan.getUnit());
        updatedPlan.setFeatures(basicPlan.getFeatures());
        updatedPlan.setUsageLimits(basicPlan.getUsageLimits());

        pricingService.updatePlanFromConfiguration(basicPlan.getName(), updatedPlan);

        Map<String, Plan> newPlans = YamlUtils.retrieveManagerFromYaml(TEMPORAL_CONFIG_PATH).getPlans();
        assertFalse(newPlans.containsKey(basicPlan.getName()));
        assertTrue(newPlans.containsKey(updatedPlan.getName()));
    }

    @Test
    void givenPlanWithNullNameShouldThrowIllegalArgumentExceptionException() {

        Plan planToUpdate = new Plan();
        planToUpdate.setName(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pricingService.updatePlanFromConfiguration("BASIC", planToUpdate));
        assertEquals("The plan null name must not be null or empty", ex.getMessage());
    }

    @Test
    void givenPlanWithEmptyStringNameShouldThrowIllegalArgumentExceptionException() {

        Plan planToUpdate = new Plan();
        planToUpdate.setName("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pricingService.updatePlanFromConfiguration("BASIC", planToUpdate));
        // FIXME Weird formatting of error message, since plan name is ""
        assertEquals("The plan  name must not be null or empty", ex.getMessage());
    }

    @Test
    void givenPlan2CharactersStringNameShouldThrowIllegalArgumentExceptionException() {

        String planName = "ab";
        Plan planToUpdate = new Plan();
        planToUpdate.setName(planName);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pricingService.updatePlanFromConfiguration("BASIC", planToUpdate));
        // FIXME Weird formatting of error message, since plan name is ""
        assertEquals("The plan " + planName + " name must have at least 3 characters", ex.getMessage());
    }

    @Test
    void givenPlan51CharactersStringNameShouldThrowIllegalArgumentExceptionException() {

        String planName = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        Plan planToUpdate = new Plan();
        planToUpdate.setName(planName);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pricingService.updatePlanFromConfiguration("BASIC", planToUpdate));
        // FIXME Weird formatting of error message, since plan name is ""
        assertEquals("The plan " + planName + " name must have at most 50 characters", ex.getMessage());
    }

    @Test
    @Disabled
    void givenPlanWithThreeSpacesInNameShouldThrowIllegalArgumentExceptionException() {

        // FIXME Plan name contains 3 space characters
        Plan planToUpdate = new Plan();
        planToUpdate.setName("   ");

        assertThrows(IllegalArgumentException.class,
                () -> pricingService.updatePlanFromConfiguration("BASIC", planToUpdate));
    }

    @Test
    @Disabled
    void givenNullPlanShouldThrowIllegalArgumentExceptionException() {
        // Check null value
        assertThrows(IllegalArgumentException.class, () -> pricingService.updatePlanFromConfiguration("BASIC", null));
    }

    @Test
    @Order(140)
    void givenNumberShouldThrowWhenUpdatingTextFeature() {

        Integer newValue = 2;

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        assertEquals(null, plan.getFeatures().get(TEST_TEXT_FEATURE).getValue(),
                TEST_TEXT_FEATURE + " from plan BASIC should be null");

        assertEquals(ValueType.TEXT, plan.getFeatures().get(TEST_TEXT_FEATURE).getValueType(),
                TEST_TEXT_FEATURE + "from plan BASIC should have TEXT as its value type");

        Feature feature = plan.getFeatures().get(TEST_TEXT_FEATURE);

        feature.setValue(newValue);

        plan.getFeatures().put(TEST_TEXT_FEATURE, feature);

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);
        });

    }

    // ---------------EDITIONS OF NONEXISTENT ATTRIBUTES ------------

    @Test
    @Order(160)
    void givenNonExistentFeatureShouldThrowIllegalArgumentException() {

        Feature unexistentFeature = new Automation();
        unexistentFeature.setName("unexistentFeature");
        unexistentFeature.setDefaultValue("testValue");
        unexistentFeature.setValueType(ValueType.TEXT);

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        plan.getFeatures().put(unexistentFeature.getName(), unexistentFeature);

        assertThrows(FeatureNotFoundException.class, () -> {
            pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);
        });

    }

    // --------------------------- FEATURES ADITION ---------------------------

    @Test
    @Order(170)
    void givenNewFeatureShouldUpdateAllPlanValues() {

        Domain newFeature = new Domain();
        newFeature.setName(NEW_FEATURE_NAME);
        newFeature.setDefaultValue(NEW_FEATURE_TEST_VALUE);
        newFeature.setValueType(ValueType.NUMERIC);
        newFeature.setExpression(NEW_FEATURE_TEST_EXPRESSION);

        pricingService.addFeatureToConfiguration(newFeature);

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertTrue(pricingManager.getFeatures().containsKey(NEW_FEATURE_NAME));
        assertTrue(pricingManager.getPlans().get("BASIC").getFeatures().containsKey(NEW_FEATURE_NAME));
        assertTrue(pricingManager.getPlans().get("ADVANCED").getFeatures().containsKey(NEW_FEATURE_NAME));
        assertTrue(pricingManager.getPlans().get("PRO").getFeatures().containsKey(NEW_FEATURE_NAME));

        assertEquals(NEW_FEATURE_TEST_EXPRESSION,
                pricingManager.getFeatures().get(NEW_FEATURE_NAME).getExpression());
        assertNull(pricingManager.getPlans().get("BASIC").getFeatures().get(NEW_FEATURE_NAME).getValue());
        assertEquals(NEW_FEATURE_TEST_VALUE,
                pricingManager.getPlans().get("BASIC").getFeatures().get(NEW_FEATURE_NAME).getDefaultValue());
        assertNull(pricingManager.getPlans().get("ADVANCED").getFeatures().get(NEW_FEATURE_NAME).getValue());
        assertEquals(NEW_FEATURE_TEST_VALUE,
                pricingManager.getPlans().get("ADVANCED").getFeatures().get(NEW_FEATURE_NAME).getDefaultValue());
        assertNull(pricingManager.getPlans().get("PRO").getFeatures().get(NEW_FEATURE_NAME).getValue());
        assertEquals(NEW_FEATURE_TEST_VALUE,
                pricingManager.getPlans().get("PRO").getFeatures().get(NEW_FEATURE_NAME).getDefaultValue());
    }

    @Test
    @Order(180)
    void givenExistentFeatureShouldThrowWhenAddingFeature() {

        Domain newFeature = new Domain();
        newFeature.setName(NEW_FEATURE_NAME);
        newFeature.setDefaultValue(NEW_FEATURE_TEST_VALUE);
        newFeature.setValueType(ValueType.NUMERIC);
        newFeature.setExpression(NEW_FEATURE_TEST_EXPRESSION);

        pricingService.addFeatureToConfiguration(newFeature);

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.addFeatureToConfiguration(newFeature);
        });
    }

    // --------------------------- FEATURES REMOVAL ---------------------------

    @Test
    @Order(190)
    void given_existent_feature_should_remove_feature() {

        String featureName = "maxPets";

        pricingService.removeFeatureFromConfiguration("maxPets");

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertFalse(pricingManager.getFeatures().containsKey(featureName));
        assertFalse(pricingManager.getPlans().get("BASIC").getFeatures().containsKey(featureName));
        assertFalse(pricingManager.getPlans().get("ADVANCED").getFeatures().containsKey(featureName));
        assertFalse(pricingManager.getPlans().get("PRO").getFeatures().containsKey(featureName));
    }

    @Test
    @Order(200)
    void given_non_existent_feature_should_throw_when_deleting() {

        String nonExistentFeatureName = "foo";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pricingService.removeFeatureFromConfiguration(nonExistentFeatureName);
        });

        assertEquals(
                "There is no feature with the name " + nonExistentFeatureName + " in the current pricing configuration",
                exception.getMessage());
    }

    // ------ FEATURES' EXPRESSIONS MANAGEMENT -------

    @Test
    @Order(210)
    void givenFeatureUpdateExpression() {

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Feature feature = pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE);

        feature.setExpression(NEW_FEATURE_TEST_EXPRESSION);

        pricingService.updateFeatureFromConfiguration(feature.getName(), feature);

        pricingManager = pricingContextTestImpl.getPricingManager();

        assertEquals(NEW_FEATURE_TEST_EXPRESSION,
                pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE).getExpression());
    }

    @Test
    @Order(220)
    void givenNonExistentFeatureShouldThrowWhenUpdatingExpression() {

        String nonExistentFeature = "non-existent";

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Feature feature = pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE);

        feature.setExpression(NEW_FEATURE_TEST_EXPRESSION);

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.updateFeatureFromConfiguration(nonExistentFeature, feature);
        });
    }

    // ------------ FEATURES' TYPES MANAGEMENT ---------------------

    @Test
    @Order(230)
    void givenFeatureShouldUpdateValueTypeToText() {

        ValueType newValueType = ValueType.TEXT;
        String newDefaultValue = "newDefaultValue";
        String newExpression = "";
        String newServerExpression = "";

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Feature feature = pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE);

        feature.setValueType(newValueType);
        feature.setDefaultValue(newDefaultValue);
        feature.setExpression(newExpression);
        feature.setServerExpression(newServerExpression);

        pricingService.updateFeatureFromConfiguration(feature.getName(), feature);

        pricingManager = pricingContextTestImpl.getPricingManager();
        feature = pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE);
        Map<String, Plan> plans = pricingManager.getPlans();

        assertEquals(ValueType.TEXT, feature.getValueType());
        assertEquals(newDefaultValue, feature.getDefaultValue());
        assertEquals(newExpression, feature.getExpression());
        assertEquals(newServerExpression, feature.getServerExpression());
        assertNull(plans.get("BASIC").getFeatures().get(TEST_NUMERIC_FEATURE).getValue());
        assertNull(plans.get("ADVANCED").getFeatures().get(TEST_NUMERIC_FEATURE).getValue());
        assertNull(plans.get("PRO").getFeatures().get(TEST_NUMERIC_FEATURE).getValue());

    }

    @Test
    @Order(240)
    void givenFeatureShouldUpdateValueTypeToBoolean() {

        ValueType newValueType = ValueType.BOOLEAN;
        Boolean newDefaultValue = false;
        String newExpression = "";
        String newServerExpression = "";

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Feature feature = pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE);

        feature.setValueType(newValueType);
        feature.setDefaultValue(newDefaultValue);
        feature.setExpression(newExpression);
        feature.setServerExpression(newServerExpression);

        pricingService.updateFeatureFromConfiguration(feature.getName(), feature);

        pricingManager = pricingContextTestImpl.getPricingManager();
        feature = pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE);
        Map<String, Plan> plans = pricingManager.getPlans();

        assertEquals(ValueType.BOOLEAN, feature.getValueType());
        assertEquals(newDefaultValue, feature.getDefaultValue());
        assertEquals(newExpression, feature.getExpression());
        assertEquals(newServerExpression, feature.getServerExpression());
        assertNull(plans.get("BASIC").getFeatures().get(TEST_NUMERIC_FEATURE).getValue());
        assertNull(plans.get("ADVANCED").getFeatures().get(TEST_NUMERIC_FEATURE).getValue());
        assertNull(plans.get("PRO").getFeatures().get(TEST_NUMERIC_FEATURE).getValue());
    }

    @Test
    @Order(250)
    void givenFeatureWithIncorrectExpressionShouldNotUpdateValueTypeToBoolean() {

        ValueType newValueType = ValueType.BOOLEAN;
        Boolean newDefaultValue = false;

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Feature feature = pricingManager.getFeatures().get(TEST_NUMERIC_FEATURE);

        feature.setValueType(newValueType);
        feature.setDefaultValue(newDefaultValue);

        String featureName = feature.getName();

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.updateFeatureFromConfiguration(featureName, feature);
        });
    }

    @Test
    @Order(260)
    void givenPriceShouldUpdateMonthlyPrice() {

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        plan.setMonthlyPrice(1000.0);
        plan.setAnnualPrice(500.0);

        pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);

        pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertEquals(1000.0, pricingManager.getPlans().get(TEST_PLAN).getMonthlyPrice());
        assertEquals(500.0, pricingManager.getPlans().get(TEST_PLAN).getAnnualPrice());
    }

    @Test
    @Order(270)
    void negativeChangePlanPriceTest() {

        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Plan plan = pricingManager.getPlans().get(TEST_PLAN);

        plan.setMonthlyPrice(-1000.0);
        plan.setAnnualPrice(500.0);

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.updatePlanFromConfiguration(TEST_PLAN, plan);
        });
    }

    @Disabled
    @Test
    @Order(270)
    void givenNewNameToFeatureShouldUpdateOnlyName() {
        PricingManager pricingManager = pricingContextTestImpl.getPricingManager();

        Feature originalFeature = pricingManager.getFeatures().get("maxPets");

        Feature featureToUpdate = pricingManager.getFeatures().get("maxPets");
        featureToUpdate.setName("foo");

        pricingService.updateFeatureFromConfiguration(originalFeature.getName(), featureToUpdate);

        PricingManager newPricingManager = YamlUtils.retrieveManagerFromYaml(TEMPORAL_CONFIG_PATH);
        Feature newFeature = newPricingManager.getFeatures().get(featureToUpdate.getName());

        assertNotEquals(originalFeature.getName(), newFeature.getName());
        assertEquals(originalFeature.getDefaultValue(), newFeature.getDefaultValue());
        assertEquals(originalFeature.getValue(), newFeature.getValue());
        assertEquals(originalFeature.getDescription(), newFeature.getDescription());
        assertEquals(originalFeature.getExpression(), newFeature.getExpression());
        assertEquals(originalFeature.getServerExpression(), newFeature.getExpression());
        assertEquals(originalFeature.getValueType(), newFeature.getValueType());

    }

    // --------------- USAGE LIMITS' MANAGEMENT ----------------

    @Test
    @Order(280)
    void shouldAddUsageLimit() {

        pricingService.addUsageLimitToConfiguration(newUsageLimit);

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        UsageLimit createdUsageLimit = pricingManager.getUsageLimits().get(newUsageLimit.getName());

        assertEquals("New usage limit description", createdUsageLimit.getDescription());
        assertEquals(ValueType.NUMERIC, createdUsageLimit.getValueType());
        assertEquals(10, createdUsageLimit.getDefaultValue());
        assertEquals("appointment", createdUsageLimit.getUnit());
        assertTrue(createdUsageLimit.getLinkedFeatures().contains(TEST_BOOLEAN_FEATURE));
    }

    @Test
    @Order(290)
    void shouldUpdateUsageLimit() {

        pricingService.addUsageLimitToConfiguration(newUsageLimit);

        newUsageLimit.setDefaultValue(20);
        newUsageLimit.setUnit("day");
        newUsageLimit.getLinkedFeatures().add(TEST_TEXT_FEATURE);

        pricingService.updateUsageLimitFromConfiguration(newUsageLimit.getName(), newUsageLimit);

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        UsageLimit updatedUsageLimit = pricingManager.getUsageLimits().get(newUsageLimit.getName());

        assertEquals("New usage limit description", updatedUsageLimit.getDescription());
        assertEquals(ValueType.NUMERIC, updatedUsageLimit.getValueType());
        assertEquals(20, updatedUsageLimit.getDefaultValue());
        assertEquals("day", updatedUsageLimit.getUnit());
        assertTrue(updatedUsageLimit.getLinkedFeatures().contains(TEST_BOOLEAN_FEATURE));
        assertTrue(updatedUsageLimit.getLinkedFeatures().contains(TEST_TEXT_FEATURE));
    }

    @Test
    @Order(290)
    void negativeShouldUpdateUsageLimit() {

        pricingService.addUsageLimitToConfiguration(newUsageLimit);

        newUsageLimit.setDefaultValue("test");

        String newUsageLimitName = newUsageLimit.getName();

        InvalidDefaultValueException exception = assertThrows(InvalidDefaultValueException.class, () -> {
            pricingService.updateUsageLimitFromConfiguration(newUsageLimitName, newUsageLimit);
        });

        assertEquals(
                "The usage limit " + newUsageLimit.getName()
                        + " defaultValue must be one of the supported numeric types if valueType is NUMERIC",
                exception.getMessage());
    }

    @Test
    @Order(300)
    void shouldRemoveUsageLimit() {

        pricingService.addUsageLimitToConfiguration(newUsageLimit);

        pricingService.removeUsageLimitFromConfiguration(newUsageLimit.getName());

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertFalse(pricingManager.getUsageLimits().containsKey(newUsageLimit.getName()));
    }

    // --------------- ADD-ONS' MANAGEMENT ----------------

    @Test
    @Order(310)
    void shouldAddAddOn() {

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertNull(pricingManager.getAddOns());

        pricingService.addAddOnToConfiguration(newAddOn);

        pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertEquals(1, pricingManager.getAddOns().size());
    }

    @Test
    @Order(320)
    void shouldNotAddRepeatedAddOn() {

        pricingService.addAddOnToConfiguration(newAddOn);

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertEquals(1, pricingManager.getAddOns().size());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pricingService.addAddOnToConfiguration(newAddOn);
        });

        assertEquals(
                "An add-on with the name " + newAddOn.getName() + " already exists within the pricing configuration",
                exception.getMessage());
    }

    @Test
    @Order(330)
    void shouldUpdateAddOn() {

        pricingService.addAddOnToConfiguration(newAddOn);

        newAddOn.setMonthlyPrice(10.0);
        newAddOn.setAnnualPrice(100.0);
        newAddOn.setUnit("owner/year");

        pricingService.updateAddOnFromConfiguration(newAddOn.getName(), newAddOn);

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        AddOn updatedAddOn = pricingManager.getAddOns().get(newAddOn.getName());

        assertEquals(10.0, updatedAddOn.getMonthlyPrice());
        assertEquals(100.0, updatedAddOn.getAnnualPrice());
        assertEquals("owner/year", updatedAddOn.getUnit());
    }

    @Test
    @Order(340)
    void shouldRemoveAddOn() {

        pricingService.addAddOnToConfiguration(newAddOn);

        pricingService.removeAddOnFromConfiguration(newAddOn.getName());

        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(pricingContextTestImpl.getConfigFilePath());

        assertFalse(pricingManager.getAddOns().containsKey(newAddOn.getName()));
    }

    // --------------- PRICING CONFIGURATION MANAGEMENT ---------------

    @Test
    @Order(10080)
    void changePricingConfigurationTest() {
        assertDoesNotThrow(() -> {
            pricingService.setPricingConfiguration(ORIGINAL_PRICING_MANAGER);
        });
    }

}

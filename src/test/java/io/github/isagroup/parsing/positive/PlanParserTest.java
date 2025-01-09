package io.github.isagroup.parsing.positive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.isagroup.models.Feature;
import io.github.isagroup.models.PricingManager;
import io.github.isagroup.services.yaml.YamlUtils;

public class PlanParserTest {

    private static final String TEST_CASES = "parsing/positive/plan/";

    @Test
    @DisplayName(value = "A plan should override the 'defaultValue' of a feature")
    void givenAFeatureShouldOverrideInAPlan() {

        String featName = "featureA";
        PricingManager pricingManager = YamlUtils
                .retrieveManagerFromYaml(TEST_CASES + "features/overwrite-defaultValue.yml");
        Feature feat = pricingManager.getPlans().get("BASIC").getFeatures().get(featName);

        assertNotNull(feat);
        assertEquals(true, feat.getValue());
    }

    @Test
    @DisplayName(value = "A plan should override a payment feature")
    void givenPaymentFeatureBasicPlanValueShouldBeAList() {

        PricingManager pricingManager = YamlUtils
                .retrieveManagerFromYaml(TEST_CASES + "features/override-payment-feature.yml");

        List<String> overwrittenPaymentMethods = (List<String>) pricingManager.getPlans().get("BASIC")
                .getFeatures()
                .get("payment")
                .getValue();

        assertInstanceOf(List.class, overwrittenPaymentMethods,
                "Payment methods is not a list of payment methods");
        assertEquals(2, overwrittenPaymentMethods.size());
        assertTrue(overwrittenPaymentMethods.contains("CARD"));
        assertTrue(overwrittenPaymentMethods.contains("GATEWAY"));
    }

    @Test
    @DisplayName(value = "A plan with no overrides must have features 'defaultValue'")
    void givenNullFeaturesBasicPlanShouldHaveDefaultValues() {

        String featName = "featureA";
        PricingManager pricingManager = YamlUtils
                .retrieveManagerFromYaml(
                        TEST_CASES + "features/null-features-defaults-to-root-features.yml");
        Feature originalFeature = pricingManager.getFeatures().get(featName);
        assertFalse(pricingManager.getPlans().get("BASIC").getFeatures().isEmpty());

        Feature feat = pricingManager.getPlans().get("BASIC").getFeatures().get(featName);

        assertInstanceOf(Boolean.class, feat.getDefaultValue());
        assertEquals(originalFeature.getDefaultValue(), feat.getDefaultValue());
    }

    @Test
    @DisplayName(value = "When 'private' is true, plan should parse")
    void givenEnablePrivateInPlanShouldParse() {

        String planName = "BASIC";
        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(TEST_CASES + "private/is-private.yml");
        assertTrue(pricingManager.getPlans().get(planName).getIsPrivate());

    }

    @Test
    @DisplayName(value = "When 'private' is false in plan should parse")
    void givenDisablePrivateInAddOnShouldParse() {

        String planName = "BASIC";
        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(TEST_CASES + "private/is-not-private.yml");
        assertFalse(pricingManager.getPlans().get(planName).getIsPrivate());

    }

    @Test
    @DisplayName(value = "When plan 'private' is not provided by default is false")
    void givenNullPrivateShouldParse() {

        String planName = "BASIC";
        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(TEST_CASES + "private/no-private.yml");
        assertFalse(pricingManager.getPlans().get(planName).getIsPrivate());

    }

    @Test
    @DisplayName(value = "When plan 'highlight' is not provided by default is false")
    void givenNullHighlightShouldParse() {

        String planName = "BASIC";
        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(TEST_CASES + "highlight/no-highlight.yml");
        assertFalse(pricingManager.getPlans().get(planName).getHighlight());

    }

    @Test
    @DisplayName(value = "One plan should be highlighted")
    void givenHighlightShouldParse() {

        String planName = "PRO";
        PricingManager pricingManager = YamlUtils.retrieveManagerFromYaml(TEST_CASES + "highlight/highlight.yml");
        assertTrue(pricingManager.getPlans().get(planName).getHighlight());

    }

}

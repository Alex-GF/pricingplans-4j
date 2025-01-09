package io.github.isagroup.services.parsing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.isagroup.exceptions.CloneFeatureException;
import io.github.isagroup.exceptions.CloneUsageLimitException;
import io.github.isagroup.exceptions.FeatureNotFoundException;
import io.github.isagroup.exceptions.InvalidDefaultValueException;
import io.github.isagroup.exceptions.PricingParsingException;
import io.github.isagroup.models.Feature;
import io.github.isagroup.models.Plan;
import io.github.isagroup.models.PricingManager;
import io.github.isagroup.models.UsageLimit;
import io.github.isagroup.models.featuretypes.Payment;
import io.github.isagroup.models.featuretypes.PaymentType;
import io.github.isagroup.utils.PricingValidators;

public class PlanParser {

    private PlanParser() {
    }

    public static Plan parseMapToPlan(String planName, Map<String, Object> map, PricingManager pricingManager) {

        Plan plan = new Plan();

        // ---------- name ----------
        
        if (planName == null) {
            throw new PricingParsingException("A plan name cannot be null");
        }

        plan.setName(planName);

        // ---------- description ----------
        
        plan.setDescription((String) map.get("description"));
        
        // ---------- private ----------

        if (map.get("private") != null) {
            if (!(map.get("private") instanceof Boolean)) {
                throw new PricingParsingException("The field \"private\" should be a boolean");
            }
            
            plan.setIsPrivate((Boolean) map.get("private"));
        }else{
            plan.setIsPrivate(false);
        }

        // ---------- price ----------

        PricingValidators.checkPriceType(map.get("price"), planName);


        if (map.get("price") instanceof String && map.get("price").toString().contains("#")) {
            plan.setPrice(PricingManagerParser.evaluateFormula(map.get("price").toString(), pricingManager));
        } else {
            plan.setPrice(map.get("price"));
        }

        // ---------- highlight ----------
        if (map.get("highlight") == null) {
            map.put("highlight", false);
        } else if (!(map.get("highlight") instanceof Boolean)) {
            throw new PricingParsingException("plan " + planName + " received a "
                    + map.get("highlight").getClass().getSimpleName() + " but was expecting a bool");
        }
        plan.setHighlight((Boolean) map.get("highlight"));

        plan.setUnit((String) map.get("unit"));

        // ---------- features ----------

        setFeaturesToPlan(planName, map, pricingManager, plan);
        
        // ---------- usageLimits ----------

        setUsageLimitsToPlan(planName, map, pricingManager, plan);


        return plan;
    }

    private static void setFeaturesToPlan(String planName, Map<String, Object> map, PricingManager pricingManager, Plan plan) {
        Map<String, Object> planFeaturesMap = (Map<String, Object>) map.get("features");
        Map<String, Feature> globalFeaturesMap = pricingManager.getFeatures();
        Map<String, Feature> planFeatures = new LinkedHashMap<>();

        if (globalFeaturesMap == null) {
            throw new IllegalArgumentException("The pricing manager does not have any features");
        }

        for (String globalFeatureName : globalFeaturesMap.keySet()) {
            Feature globalFeature = globalFeaturesMap.get(globalFeatureName);
            try {
                planFeatures.put(globalFeatureName, Feature.cloneFeature(globalFeature));
            } catch (CloneFeatureException e) {
                throw new CloneFeatureException("Error while clonnig the feature " + globalFeatureName);
            }
        }

        plan.setFeatures(planFeatures);

        if (planFeaturesMap == null) {
            return;
        }

        for (String planFeatureName : planFeaturesMap.keySet()) {

            Map<String, Object> planFeatureMap = (Map<String, Object>) planFeaturesMap.get(planFeatureName);

            if (!plan.getFeatures().containsKey(planFeatureName)) {
                throw new FeatureNotFoundException(
                    "The feature " + planFeatureName + " is not defined in the global features");
            } else {
                Feature feature = plan.getFeatures().get(planFeatureName);

                switch (feature.getValueType()) {
                    case NUMERIC:
                        feature.setValue(planFeatureMap.get("value"));
                        if (!(feature.getValue() instanceof Integer || feature.getValue() instanceof Double
                            || feature.getValue() instanceof Long)) {
                            throw new InvalidDefaultValueException("The feature " + feature.getName()
                                + " does not have a valid value. Current valueType: "
                                + feature.getValueType().toString() + "; Current value in " + plan.getName() + ": "
                                + planFeatureMap.get("value").toString());
                        }
                        break;
                    case BOOLEAN:
                        if (!(planFeatureMap.get("value") instanceof Boolean)) {
                            throw new InvalidDefaultValueException("The feature " + feature.getName()
                                + " does not have a valid value. Current valueType: "
                                + feature.getValueType().toString() + "; Current value in " + plan.getName() + ": "
                                + planFeatureMap.get("value").toString());
                        }
                        feature.setValue((boolean) planFeatureMap.get("value"));
                        break;
                    case TEXT:

                        if (feature instanceof Payment) {
                            parsePaymentValue(feature, planFeatureName, planFeatureMap);
                        } else {
                            if (!(planFeatureMap.get("value") instanceof String)) {
                                throw new InvalidDefaultValueException("The feature " + feature.getName()
                                    + " does not have a valid value. Current valueType: "
                                    + feature.getValueType().toString() + "; Current value in " + plan.getName()
                                    + ": " + planFeatureMap.get("value").toString());
                            }
                            feature.setValue((String) planFeatureMap.get("value"));
                        }
                        break;
                }

                plan.getFeatures().put(planFeatureName, feature);
            }
        }
    }

    private static void setUsageLimitsToPlan(String planName, Map<String, Object> map, PricingManager pricingManager,
                                             Plan plan) {
        Map<String, Object> planUsageLimitsMap = (Map<String, Object>) map.get("usageLimits");
        Map<String, UsageLimit> globalUsageLimitsMap = pricingManager.getUsageLimits();
        Map<String, UsageLimit> planUsageLimits = new LinkedHashMap<>();

        if (globalUsageLimitsMap == null) {
            return;
        }

        for (String globalUsageLimitName : globalUsageLimitsMap.keySet()) {
            UsageLimit globalUsageLimit = globalUsageLimitsMap.get(globalUsageLimitName);
            try {
                planUsageLimits.put(globalUsageLimitName, UsageLimit.cloneUsageLimit(globalUsageLimit));
            } catch (CloneUsageLimitException e) {
                throw new CloneUsageLimitException("Error while clonnig the usageLimit " + globalUsageLimitName);
            }
        }

        plan.setUsageLimits(planUsageLimits);

        if (planUsageLimitsMap == null) {
            return;
        }

        for (String planUsageLimitName : planUsageLimitsMap.keySet()) {

            Map<String, Object> planUsageLimitMap = (Map<String, Object>) planUsageLimitsMap.get(planUsageLimitName);

            if (!plan.getUsageLimits().containsKey(planUsageLimitName)) {
                throw new FeatureNotFoundException(
                    "The usageLimit " + planUsageLimitName + " is not defined in the global usageLimits");
            } else {
                UsageLimit usageLimit = plan.getUsageLimits().get(planUsageLimitName);

                switch (usageLimit.getValueType()) {
                    case NUMERIC:
                        usageLimit.setValue(planUsageLimitMap.get("value"));
                        if (!(usageLimit.getValue() instanceof Integer || usageLimit.getValue() instanceof Double
                            || usageLimit.getValue() instanceof Long || usageLimit.getValue() == null)) {
                            throw new InvalidDefaultValueException(
                                "The usageLimit " + planUsageLimitName
                                    + " does not have a valid value. Current valueType:"
                                    + usageLimit.getValueType().toString() + "; Current defaultValue: "
                                    + planUsageLimitMap.get("value").toString());
                        }
                        break;
                    case BOOLEAN:
                        usageLimit.setValue((Boolean) planUsageLimitMap.get("value"));
                        break;
                    case TEXT:
                        usageLimit.setValue((String) planUsageLimitMap.get("value"));
                        break;
                }

                if (usageLimit.getValue() == null) {
                    throw new InvalidDefaultValueException(
                        "The usageLimit " + planUsageLimitName + " does not have a valid value in the plan "
                            + planName + ". The actual value is null");
                }

                plan.getUsageLimits().put(planUsageLimitName, usageLimit);
            }
        }
    }

    public static void parsePaymentValue(Feature feature, String featureName, Map<String, Object> map) {

        Object paymentValue = map.get("value");
        if (paymentValue instanceof String) {
            throw new PricingParsingException(
                "\"" + featureName + "\"" + "should be a list of supported payment types");
        }

        List<String> allowedPaymentTypes = (List<String>) paymentValue;
        for (String type : allowedPaymentTypes) {
            try {
                PaymentType.valueOf(type);
            } catch (IllegalArgumentException e) {
                throw new InvalidDefaultValueException("The feature " + featureName
                    + " does not have a supported paymentType. PaymentType that generates the issue: " + type);
            }
        }

        feature.setValue(allowedPaymentTypes);

    }

    private static boolean isValidPrice(Object price) {
        return price instanceof Double || price instanceof Long || price instanceof Integer || price instanceof String || price == null;
    }

}

package io.github.isagroup.services.parsing;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import io.github.isagroup.exceptions.PricingParsingException;
import io.github.isagroup.exceptions.VersionException;
import io.github.isagroup.models.AddOn;
import io.github.isagroup.models.Feature;
import io.github.isagroup.models.Plan;
import io.github.isagroup.models.PricingManager;
import io.github.isagroup.models.UsageLimit;
import io.github.isagroup.services.updaters.Version;

public class PricingManagerParser {

    private PricingManagerParser() {
    }

    public static PricingManager parseMapToPricingManager(Map<String, Object> yamlConfigMap) {

        PricingManager pricingManager = new PricingManager();

        setBasicAttributes(yamlConfigMap, pricingManager);
        setFeatures(yamlConfigMap, pricingManager);
        setUsageLimits(yamlConfigMap, pricingManager);
        setPlans(yamlConfigMap, pricingManager);
        setAddOns(yamlConfigMap, pricingManager);

        if (pricingManager.getPlans() == null && pricingManager.getAddOns() == null) {
            throw new PricingParsingException("The pricing manager does not have any plans or add ons");
        }

        return pricingManager;
    }

    protected static Double evaluateFormula(String price, PricingManager pricingManager) {
        SpelExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        if (pricingManager.getVariables() != null) {
            context.setVariables(pricingManager.getVariables());
        }

        Expression priceExpression = parser.parseExpression(price);
        return priceExpression.getValue(context, Double.class);
    }

    private static void setBasicAttributes(Map<String, Object> yamlConfigMap, PricingManager pricingManager) {

        // -------------- version --------------

        Version version = null;

        try {
            if (yamlConfigMap.get("version") == null) {
                throw new PricingParsingException(
                        "The version field of the pricing must not be null or undefined. Please ensure that the version field is present and correctly formatted");
            } else if (yamlConfigMap.get("version") instanceof Double
                    || yamlConfigMap.get("version") instanceof String) {

                version = Version.version(yamlConfigMap.get("version"));

            } else {
                throw new PricingParsingException(
                        String.format("'version' detected type is %s but 'version' type must be Double or String",
                                yamlConfigMap.get("version").getClass().getSimpleName()));
            }
        } catch (VersionException e) {
            throw new PricingParsingException(e.getMessage());
        }

        pricingManager.setVersion(version);

        // -------------- saasName --------------

        if (yamlConfigMap.get("saasName") == null) {
            throw new PricingParsingException("SaasName was not defined");
        }

        if (!(yamlConfigMap.get("saasName") instanceof String)) {
            throw new PricingParsingException("'saasName' has to be a string");
        }

        if (((String) yamlConfigMap.get("saasName")).length() > 50 || ((String) yamlConfigMap.get("saasName")).length() < 3) {
            throw new PricingParsingException("The 'saasName' field must be a String with a length between 3 and 50 characters");
        }

        pricingManager.setSaasName((String) yamlConfigMap.get("saasName"));

        // -------------- url --------------

        if (yamlConfigMap.get("url") != null && !(yamlConfigMap.get("url") instanceof String)) {
            throw new PricingParsingException("If 'url' field is used, it has to be a string");
        }

        if (yamlConfigMap.get("url") != null) {
            String pricingUrl = (String) yamlConfigMap.get("url");
        
            if (!pricingUrl.matches("^(http|https)://.*$")) {
                throw new PricingParsingException("The 'url' field must be a String with a valid URL format, using http or https protocol");
            }

            pricingManager.setUrl((String) yamlConfigMap.get("url"));
        }

        // -------------- createdAt --------------

        if (yamlConfigMap.get("createdAt") == null) {
            throw new PricingParsingException("'createdAt' is mandatory. Check your config file.");
        } else if (yamlConfigMap.get("createdAt") instanceof Date) {
            Date createdAt = (Date) yamlConfigMap.get("createdAt");
            pricingManager.setCreatedAt(createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        } else if (yamlConfigMap.get("createdAt") instanceof String) {
            try {
                pricingManager.setCreatedAt(LocalDate.parse((String) yamlConfigMap.get("createdAt")));

            } catch (DateTimeParseException err) {
                throw new PricingParsingException(
                        String.format(
                                "\"createdAt\" \"%s\" is invalid, use the following yyyy-MM-dd",
                                yamlConfigMap.get("createdAt")));
            }

        } else {
            throw new PricingParsingException(
                    String.format(
                            "\"createdAt\" detected type is %s and must be a String or Date formatted like yyyy-MM-dd",
                            yamlConfigMap.get("createdAt").getClass().getSimpleName()));
        }

        // -------------- currency --------------

        if (yamlConfigMap.get("currency") == null) {
            throw new PricingParsingException("Currency was not defined");
        }

        if (!(yamlConfigMap.get("currency") instanceof String)) {
            throw new PricingParsingException("'currency' has to be a string");
        }

        pricingManager.setCurrency((String) yamlConfigMap.get("currency"));

        // -------------- tags --------------

        if (yamlConfigMap.get("tags") != null && !(yamlConfigMap.get("tags") instanceof List)) {
            throw new PricingParsingException(String.format("\"tags\" type is %s and must be a List",
                    yamlConfigMap.get("tags").getClass().getSimpleName()));
        }

        if (yamlConfigMap.get("tags") != null) {
            pricingManager.setTags((List<String>) yamlConfigMap.get("tags"));
        }

        // -------------- variables --------------
        checkVariables(yamlConfigMap);
        pricingManager.setVariables((Map<String, Object>) yamlConfigMap.get("variables"));

        // -------------- billing --------------

        if (yamlConfigMap.get("billing") != null) {
            if (!(yamlConfigMap.get("billing") instanceof Map)) {
                throw new PricingParsingException(
                        String.format("\"billing\" type is %s and must be a Map",
                                yamlConfigMap.get("billing").getClass().getSimpleName()));
            }

            Map<String, Object> billingMap = (Map<String, Object>) yamlConfigMap.get("billing");
            Map<String, Double> parsedBilling = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : billingMap.entrySet()) {

                if (entry.getValue() == null) {
                    throw new PricingParsingException(String.format("The billing key '%s' cannot have a null value", entry.getKey()));
                }

                if (!(entry.getValue() instanceof Double || entry.getValue() instanceof Integer)) {
                    throw new PricingParsingException(
                            String.format("The value for billing key '%s' must be a Double or an Integer, but found %s",
                                    entry.getKey(), entry.getValue().getClass().getSimpleName()));
                }
                
                if (entry.getValue() instanceof Integer) {
                    entry.setValue(((Integer) entry.getValue()).doubleValue());
                }

                if ((Double) entry.getValue() <= 0 || (Double) entry.getValue() > 1) {
                    throw new PricingParsingException(
                            String.format("The value for billing key '%s' must be a coeficient in the range (0,1], but found %s", entry.getKey(), entry.getValue()));
                }

                parsedBilling.put(entry.getKey(), (Double) entry.getValue());
            }

            pricingManager.setBilling(parsedBilling);
        }else{
            Map<String, Double> billingMap = new LinkedHashMap<>();
            billingMap.put("monthly", 1.0);
            pricingManager.setBilling(billingMap);
        }

    }

    private static void checkVariables(Map<String, Object> yamlConfigMap) {
        Pattern pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");
        if (yamlConfigMap.get("variables") != null && !(yamlConfigMap.get("variables") instanceof Map)) {
            throw new PricingParsingException("variables must be a map but found "
                    + yamlConfigMap.get("variables").getClass().getSimpleName() + " instead");
        }

        if (yamlConfigMap.get("variables") != null && yamlConfigMap.get("variables") instanceof Map) {
            for (Object key : ((Map<?, ?>) yamlConfigMap.get("variables")).keySet()) {
                if (key == null) {
                    throw new PricingParsingException("null is not a valid key for a variable");
                }
                if (!pattern.matcher(key.toString()).matches()) {
                    throw new PricingParsingException(key + " is not a valid name for a variable. ");
                }
            }
        }

    }

    private static void setFeatures(Map<String, Object> map, PricingManager pricingManager) {

        if (map.get("features") == null) {
            throw new PricingParsingException(
                    "'features' is mandatory. It should be a map of features with their correspoding attributes.");
        }

        if (!(map.get("features") instanceof Map)) {
            throw new PricingParsingException(
                    "'features' must be a Map but found " + map.get("features").getClass().getSimpleName()
                            + " instead");
        }

        Map<String, Feature> pricingFeatures = new LinkedHashMap<>();
        Map<String, Object> featuresMap = (Map<String, Object>) map.get("features");

        featuresMap.entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof Map))
                .findFirst()
                .ifPresent(entry -> {
                    throw new PricingParsingException(
                            String.format("Feature '%s' must be a Map but found %s instead",
                                    entry.getKey(), entry.getValue().getClass().getSimpleName()));
                });

        for (String featureName : featuresMap.keySet()) {

            try {
                Map<String, Object> featureMap = (Map<String, Object>) featuresMap.get(featureName);
                Feature feature = FeatureParser.parseMapToFeature(featureName, featureMap, pricingManager);
                pricingFeatures.put(featureName, feature);
            } catch (ClassCastException e) {
                throw new PricingParsingException(
                        "An error has occurred while parsing the feature " + featureName + ". Error: "
                                + e.getMessage());
            }
        }

        pricingManager.setFeatures(pricingFeatures);
    }

    private static void setUsageLimits(Map<String, Object> map, PricingManager pricingManager) {
        Map<String, Object> usageLimitsMap = (Map<String, Object>) map.get("usageLimits");
        Map<String, UsageLimit> usageLimits = new LinkedHashMap<>();

        if (usageLimitsMap == null) {
            return;
        }

        for (String limitName : usageLimitsMap.keySet()) {
            try {
                Map<String, Object> limitMap = (Map<String, Object>) usageLimitsMap.get(limitName);
                UsageLimit limit = UsageLimitParser.parseMapToUsageLimit(limitName, limitMap, pricingManager);

                usageLimits.put(limitName, limit);
            } catch (ClassCastException e) {
                throw new PricingParsingException(
                        "An error has occurred while parsing the usage limit " + limitName + ". Error: "
                                + e.getMessage());
            }
        }

        pricingManager.setUsageLimits(usageLimits);
    }

    private static void setPlans(Map<String, Object> map, PricingManager pricingManager) {

        Map<String, Object> plansMap = new HashMap<>();

        try {
            plansMap = (Map<String, Object>) map.get("plans");
        } catch (ClassCastException e) {
            throw new PricingParsingException(
                    "The plans are not defined correctly. It should be a map of plans and their options");
        }

        Map<String, Plan> plans = new LinkedHashMap<>();

        if (plansMap == null) {
            return;
        }

        for (String planName : plansMap.keySet()) {
            try {
                Map<String, Object> planMap = (Map<String, Object>) plansMap.get(planName);
                Plan plan = PlanParser.parseMapToPlan(planName, planMap, pricingManager);

                plans.put(planName, plan);
            } catch (ClassCastException e) {
                throw new PricingParsingException(
                        "An error has occurred while parsing the plan " + planName + ". Error: " + e.getMessage());
            }
        }

        if (plansHaveMultipleHighlight(plans)) {
            throw new PricingParsingException("Multiple highlighted plans are not allowed");
        }

        pricingManager.setPlans(plans);
    }

    private static boolean plansHaveMultipleHighlight(Map<String, Plan> plans) {
        return plans.entrySet().stream().filter(plan -> plan.getValue().getHighlight()).count() > 1;
    }

    private static void setAddOns(Map<String, Object> map, PricingManager pricingManager) {
        Map<String, Object> addOnsMap = (Map<String, Object>) map.get("addOns");

        if (addOnsMap == null) {
            return;
        }

        pricingManager.setAddOns(new LinkedHashMap<>());

        for (String addOnName : addOnsMap.keySet()) {
            try {
                Map<String, Object> addOnMap = (Map<String, Object>) addOnsMap.get(addOnName);
                AddOn addOn = AddOnParser.parseMapToAddOn(addOnName, addOnMap, pricingManager);

                pricingManager.getAddOns().put(addOnName, addOn);
            } catch (ClassCastException e) {
                throw new PricingParsingException(
                        "An error has occurred while parsing the add-on " + addOnName + ". Error: " + e.getMessage());
            }
        }

        for (String addOnName : addOnsMap.keySet()) {
            try {
                AddOn parsedAddOn = pricingManager.getAddOns().get(addOnName);
                Map<String, Object> addOnMap = (Map<String, Object>) addOnsMap.get(addOnName);

                AddOnParser.setDependsOn(addOnMap, pricingManager, parsedAddOn);
                AddOnParser.setExcludes(addOnMap, pricingManager, parsedAddOn);

                pricingManager.getAddOns().put(addOnName, parsedAddOn);
            } catch (Exception e) {
                throw new PricingParsingException(
                        "An error has occurred while parsing the add-on " + addOnName + ". Error: " + e.getMessage());
            }
        }
    }
}

package io.github.isagroup.models;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Object to model pricing plans
 */
@Getter
@Setter
@EqualsAndHashCode
public class Plan {
    private String name;
    private String description;
    private Object price;
    private String unit;
    private Boolean isPrivate;
    private Boolean highlight;
    private Map<String, Feature> features;
    private Map<String, UsageLimit> usageLimits;

    public Map<String, Object> parseToMap(){
        Map<String, Object> planMap = new LinkedHashMap<>();
        planMap.put("name", name);
        planMap.put("description", description);
        planMap.put("price", price);
        planMap.put("unit", unit);
        planMap.put("isPrivate", isPrivate);
        planMap.put("features", features);
        planMap.put("usageLimits", usageLimits);
        return planMap;
    }

    public Map<String, Object> serializePlan() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("description", description);
        attributes.put("price", price);
        attributes.put("unit", unit);
        attributes.put("private", isPrivate);
        attributes.put("highlight", highlight);

        Map<String, Object> features = serializeFeatures().orElse(null);
        Map<String, Object> usageLimits = serializeUsageLimits().orElse(null);

        attributes.put("features", features);
        attributes.put("usageLimits", usageLimits);

        return attributes;
    }

    private <V> Optional<Map<String, V>> serializeValue(V value) {
        if (value == null) {
            return Optional.empty();
        }

        Map<String, V> attributes = new LinkedHashMap<>();
        attributes.put("value", value);
        return Optional.of(attributes);
    }

    private Optional<Map<String, Object>> serializeFeatures() {

        if (features == null) {
            return Optional.empty();
        }

        Map<String, Object> serializedFeatures = new LinkedHashMap<>();
        for (Feature feature : features.values()) {
            Optional<Map<String, Object>> serializedFeature = serializeValue(feature.getValue());
            if (serializedFeature.isPresent()) {
                serializedFeatures.put(feature.getName(), serializedFeature.get());
            }
        }

        boolean featureMapIsEmpty = serializedFeatures.size() == 0;

        if (featureMapIsEmpty) {
            return Optional.empty();
        }

        return Optional.of(serializedFeatures);
    }

    private Optional<Map<String, Object>> serializeUsageLimits() {

        if (usageLimits == null) {
            return Optional.empty();
        }

        Map<String, Object> serializedUsageLimits = new LinkedHashMap<>();

        for (UsageLimit usageLimit : usageLimits.values()) {
            Optional<Map<String, Object>> serializedUsageLimit = serializeValue(usageLimit.getValue());
            if (serializedUsageLimit.isPresent()) {

                serializedUsageLimits.put(usageLimit.getName(), serializedUsageLimit.get());
            }
        }

        boolean usageLimitMapIsEmpty = serializedUsageLimits.size() == 0;

        if (usageLimitMapIsEmpty) {
            return Optional.empty();
        }

        return Optional.of(serializedUsageLimits);
    }

    @Override
    public String toString() {
        return "Plan[name=" + name + ", price=" + price + ", unit="
                + unit + ", features: " + features.get("superAdminRole") + "]";
    }
}

package io.github.isagroup.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.isagroup.exceptions.CloneUsageLimitException;

@Getter
@Setter
@EqualsAndHashCode
public abstract class UsageLimit implements Serializable {
    private String name;
    private String description;
    private ValueType valueType;
    private Object defaultValue;
    protected UsageLimitType type;
    private String unit;
    private transient Object value;
    private List<String> linkedFeatures = new ArrayList<>();
    private String expression;
    private String serverExpression;

    public boolean isLinkedToFeature(String featureName) {
        return linkedFeatures.contains(featureName);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (description != null) {
            attributes.put("description", description);
        }

        if (valueType != null) {
            attributes.put("valueType", valueType.toString());
        }

        if (defaultValue != null) {
            attributes.put("defaultValue", defaultValue);
        }

        if (unit != null) {
            attributes.put("unit", unit);
        }

        attributes.put("type", type.toString());

        if (linkedFeatures != null && !linkedFeatures.isEmpty()) {
            attributes.put("linkedFeatures", linkedFeatures);
        }

        if (expression != null) {
            attributes.put("expression", expression);
        }

        if (serverExpression != null) {
            attributes.put("serverExpression", serverExpression);
        }

        return attributes;
    }

    public static UsageLimit cloneUsageLimit(UsageLimit original) throws CloneUsageLimitException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(original);

            // Deserializa el objeto desde el flujo de bytes, creando una copia
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

            return (UsageLimit) objectInputStream.readObject();

        } catch (Exception e) {
            throw new CloneUsageLimitException("Error cloning usageLimit");
        }
    }

}

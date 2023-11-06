package io.github.isagroup.services.yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import io.github.isagroup.models.Feature;
import io.github.isagroup.models.Plan;
import io.github.isagroup.models.PricingManager;
import io.github.isagroup.services.parsing.PricingManagerParser;

/**
 * Utility class to handle YAML files
 */
public class YamlUtils {

    /**
     * This method maps the content of the YAML file located in {@code yamlPath}
     * into a {@link PricingManager} object.
     * @param yamlPath Path of the YAML file, relative to the resources folder
     * @return PricingManager object that represents the content of the YAML file
     */
    public static PricingManager retrieveManagerFromYaml(String yamlPath){
        Yaml yaml = new Yaml();

        Map<String, Object> test = yaml.load(YamlUtils.class.getClassLoader().getResourceAsStream(yamlPath));

        // Map<String, Plan> plans = pricingManager.getPlans();
        // Map<String, Feature> globalFeatures = pricingManager.getFeatures();

        // for (String planName: plans.keySet()){
        //     Plan plan = plans.get(planName);
            
        //     Map<String, Feature> planFeatures = plan.getFeatures();

        //     for (String featureName: planFeatures.keySet()){
        //         Feature globalFeature = globalFeatures.get(featureName);
        //         Feature planFeature = planFeatures.get(featureName);
        //         planFeature.setDescription(globalFeature.getDescription());
        //         planFeature.setType(globalFeature.getType());
        //         planFeature.setDefaultValue(globalFeature.getDefaultValue());
        //         planFeature.setExpression(globalFeature.getExpression());
        //         planFeature.setServerExpression(globalFeature.getServerExpression());
        //         if (planFeature.getValue() == null){
        //             planFeature.setValue(globalFeature.getDefaultValue());
        //         }

        //         planFeatures.put(featureName, planFeature);
        //     }

        //     plan.setFeatures(planFeatures);
        //     plans.put(planName, plan);
        // }

        return PricingManagerParser.parseMapToPricingManager(test);
    }

    /**
     * Writes a {@link PricingManager} object into a YAML file.
     * @param pricingManager a {@link PricingManager} object that represents a pricing configuration
     * @param yamlPath Path of the YAML file, relative to the resources folder
     */
    public static void writeYaml(PricingManager pricingManager, String yamlPath){
        DumperOptions dump = new DumperOptions();
        dump.setIndent(2);
        dump.setPrettyFlow(true);
        dump.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(dump){
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,Tag customTag) {
                // if value of property is null, ignore it.
                if (propertyValue == null) {
                    return null;
                }  
                else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };
        representer.addClassTag(PricingManager.class, Tag.MAP);

        Map<String, Plan> plans = pricingManager.getPlans();

        for (String planName: plans.keySet()){
            Plan plan = plans.get(planName);
            
            Map<String, Feature> planFeatures = plan.getFeatures();

            for (String featureName: planFeatures.keySet()){
                Feature planFeature = planFeatures.get(featureName);
                planFeature.prepareToPlanWriting();

                planFeatures.put(featureName, planFeature);
            }

            plan.setFeatures(planFeatures);
            plans.put(planName, plan);
        }

        try {
            Yaml yaml = new Yaml(representer, dump);
            FileWriter writer = new FileWriter("src/test/resources/"+ yamlPath);
            yaml.dump(pricingManager, writer);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

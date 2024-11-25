import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ToscaToCamlTranslator {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ToscaToCamlTranslator <input-tosca-filepath> <output-caml-filepath>");
            return;
        }

        String inputFilePath = args[0];
        String outputFilePath = args[1];

        try {
            // Initialize YAML parser
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            
            // Parse TOSCA YAML
            Map<String, Object> toscaYaml = yamlMapper.readValue(new File(inputFilePath), Map.class);

            // Translate TOSCA to CAML
            Map<String, Object> camlYaml = translateToscaToCaml(toscaYaml);

            // Write the translated CAML YAML to output file
            yamlMapper.writeValue(new File(outputFilePath), camlYaml);

            System.out.println("Translation successful! Output written to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error processing YAML files: " + e.getMessage());
        }
    }

private static Map<String, Object> translateToscaToCaml(Map<String, Object> toscaYaml) {
    Map<String, Object> camlYaml = new LinkedHashMap<>();
    camlYaml.put("apiVersion", "nebulous/v1");
    camlYaml.put("kind", "MetricModel");

    // Metadata section
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("name", "Translated Metric Model");
    camlYaml.put("metadata", metadata);

    // Extract components
    List<Map<String, Object>> components = new ArrayList<>();
    Map<String, Object> nodeTypes = (Map<String, Object>) toscaYaml.get("node_types");

    if (nodeTypes != null) {
        for (Map.Entry<String, Object> entry : nodeTypes.entrySet()) {
            Map<String, Object> component = new LinkedHashMap<>();
            component.put("name", entry.getKey());

            Map<String, Object> nodeData = (Map<String, Object>) entry.getValue();
            List<Map<String, Object>> capabilities = (List<Map<String, Object>>) nodeData.get("capabilities");

            List<Map<String, Object>> metrics = new ArrayList<>();
            if (capabilities != null) {
                for (Map<String, Object> capability : capabilities) {
                    Map<String, Object> monitoring = (Map<String, Object>) capability.get("monitoring");
                    if (monitoring != null) {
                        Map<String, Object> properties = (Map<String, Object>) monitoring.get("properties");
                        if (properties != null) {
                            // Process raw metrics
                            Map<String, Object> raw = (Map<String, Object>) properties.get("raw");
                            if (raw != null) {
                                for (Map.Entry<String, Object> rawMetric : raw.entrySet()) {
                                    Map<String, Object> metric = new LinkedHashMap<>();
                                    metric.put("name", rawMetric.getKey());

                                    Map<String, Object> rawMetricData = (Map<String, Object>) rawMetric.getValue();
                                    if (rawMetricData != null) {
                                        // Sensor configuration
                                        Map<String, Object> sensor = new LinkedHashMap<>();
                                        sensor.put("type", rawMetricData.get("collector"));
                                        metric.put("sensor", sensor);

                                        // Output and frequency
                                        String collectionOutput = (String) rawMetricData.get("collection_output");
                                        String collectionFrequency = (String) rawMetricData.get("collection_frequency");
                                        metric.put("output", collectionOutput + " " + collectionFrequency);
                                    }

                                    // Add to metrics
                                    metrics.add(metric);
                                }
                            }

                            // Process composite metrics
                            Map<String, Object> composite = (Map<String, Object>) properties.get("composite");
                            if (composite != null) {
                                for (Map.Entry<String, Object> compositeMetric : composite.entrySet()) {
                                    Map<String, Object> metric = new LinkedHashMap<>();
                                    metric.put("name", compositeMetric.getKey());
                                    Map<String, Object> compositeData = (Map<String, Object>) compositeMetric.getValue();

                                    if (compositeData != null) {
                                        // Handle the formula
                                        metric.put("formula", compositeData.get("formula") != null ?
                                                compositeData.get("formula").toString() :
                                                compositeData.get("type") + "(" + compositeData.get("argument") + ")");

                                        // Handle window
                                        Map<String, Object> window = (Map<String, Object>) compositeData.get("window");
                                        if (window != null) {
                                            String windowType = (String) window.get("type");
                                            String windowSize = (String) window.get("size");
                                            metric.put("window", windowType != null ? windowType : "" + " " + windowSize);
                                        }

                                        // Add output and frequency
                                        String collectionOutput = (String) compositeData.get("collection_output");
                                        String collectionFrequency = (String) compositeData.get("collection_frequency");
                                        metric.put("output", collectionOutput + " " + collectionFrequency);
                                    }

                                    // Add to metrics
                                    metrics.add(metric);
                                }
                            }
                        }
                    }
                }
            }

            // Requirements
            List<Map<String, Object>> requirements = new ArrayList<>();
            Map<String, Object> sloRequirement = new LinkedHashMap<>();
            sloRequirement.put("name", "cpu_slo");
            sloRequirement.put("type", "slo");
            sloRequirement.put("constraint", "cpu_util_prct > 80");
            requirements.add(sloRequirement);
            component.put("requirements", requirements);

            // Add metrics to component
            component.put("metrics", metrics);
            components.add(component);
        }
    }

    camlYaml.put("spec", Map.of("components", components));

    // Scopes section
    List<Map<String, Object>> scopes = new ArrayList<>();
    Map<String, Object> scope = new LinkedHashMap<>();
    scope.put("name", "a_scope");
    scope.put("components", List.of("a_comoponent"));
    scopes.add(scope);
    camlYaml.put("scopes", scopes);

    return camlYaml;
}


}

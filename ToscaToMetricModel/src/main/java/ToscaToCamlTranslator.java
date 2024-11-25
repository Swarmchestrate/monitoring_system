import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToscaToCamlTranslator {
    private static final Logger logger = LoggerFactory.getLogger(ToscaToCamlTranslator.class);

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ToscaToCamlTranslator <input-tosca-filepath> <output-caml-filepath>");
            return;
        }

        String inputFilePath = args[0];
        String outputFilePath = args[1];

        try {
            logger.info("Initializing YAML parser");
            // Initialize YAML parser
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

            logger.info("Parsing TOSCA YAML from file: {}", inputFilePath);
            // Parse TOSCA YAML
            Map<String, Object> toscaYaml = yamlMapper.readValue(new File(inputFilePath), Map.class);
            logger.debug("Parsed TOSCA YAML: {}", toscaYaml);

            logger.info("Translating TOSCA to CAML");
            // Translate TOSCA to CAML
            Map<String, Object> camlYaml = translateToscaToCaml(toscaYaml);
            logger.debug("Translated CAML YAML: {}", camlYaml);

            logger.info("Writing translated CAML YAML to output file: {}", outputFilePath);
            // Write the translated CAML YAML to output file
            yamlMapper.writeValue(new File(outputFilePath), camlYaml);

            logger.info("Translation successful! Output written to: {}", outputFilePath);
        } catch (IOException e) {
            logger.error("Error processing YAML files: {}", e.getMessage());
        }
    }

    private static Map<String, Object> translateToscaToCaml(Map<String, Object> toscaYaml) {
        Map<String, Object> camlYaml = new LinkedHashMap<>();
        camlYaml.put("apiVersion", "swarmchestrate/v0");
        camlYaml.put("kind", "MetricModel");
        logger.info("Extracting components");
        // Extract components
        List<Map<String, Object>> componentsCaml = new ArrayList<>();

        logger.info("Creating metadata section");
        // Metadata section
        Map<String, Object> metadata = (Map<String, Object>) toscaYaml.get("metadata");
        logger.debug("Metadata: {}", metadata);
        camlYaml.put("metadata", metadata);

        Map<String, Object> componentsData = (Map<String, Object>) toscaYaml.get("node_types");
        logger.debug("Component types: {}", componentsData);

        if (componentsData != null) {
            for (Map.Entry<String, Object> entry : componentsData.entrySet()) {
                Map<String, Object> componentCaml = new LinkedHashMap<>();
                componentCaml.put("name", entry.getKey());

                Map<String, Object> componentNodeTypes = (Map<String, Object>) entry.getValue();
                
                List<Map<String, Object>> metrics = new ArrayList<>();
                List<Map<String, Object>> requirements = new ArrayList<>();

                // Loop through the node types of tge component
                for (Map.Entry<String, Object> nodeType : componentNodeTypes.entrySet()) {
                    Map<String, Object> nodeData = (Map<String, Object>) nodeType.getValue();
                    logger.debug("Node data for {}: {}", entry.getKey(), nodeData);
                    List<Map<String, Object>> capabilities = (List<Map<String, Object>>) nodeData.get("capabilities");
                    logger.debug("Capabilities for {}: {}", entry.getKey(), capabilities);

                    if (capabilities != null) {
                        for (Map<String, Object> capability : capabilities) {

                            // search for a capability which has "type" of capabilities.MetricMonitoringCapability
                            if ("capabilities.MetricMonitoringCapability".equals(capability.get("type"))) {
                                Map<String, Object> properties = (Map<String, Object>) capability.get("properties");
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

                            // Requirements
                            if ("capabilities.SloMonitoringCapability".equals(capability.get("type"))) {
                                Map<String, Object> slos = (Map<String, Object>) capability.get("properties").entrySet();
                                for (Map.Entry<String, Object> slo : slos) {
                                    Map<String, Object> sloRequirement = new LinkedHashMap<>();
                                    sloRequirement.put("name", slo.getKey());
                                    sloRequirement.put("type", "slo");
                                    sloRequirement.put("constraint", slo.getValue());
                                    requirements.add(sloRequirement);
                                }
                            }
                        }
                    }
                }

                componentCaml.put("requirements", requirements);

                // Add metrics to componentCaml
                componentCaml.put("metrics", metrics);
                componentsCaml.add(componentCaml);
            }
        }

        camlYaml.put("spec", Map.of("components", componentsCaml));
        logger.debug("Components: {}", componentsCaml);

        logger.info("Creating scopes section");
        // Scopes section
        List<Map<String, Object>> scopes = new ArrayList<>();
        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("name", "a_scope");
        scope.put("components", List.of("a_comoponent"));
        scopes.add(scope);
        camlYaml.put("scopes", scopes);
        logger.debug("Scopes: {}", scopes);

        return camlYaml;
    }
}

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
            Map<String, Object> camlYaml = translateToscaToCaml(toscaYaml, inputFilePath);
            logger.debug("Translated CAML YAML: {}", camlYaml);

            logger.info("Writing translated CAML YAML to output file: {}", outputFilePath);
            // Write the translated CAML YAML to output file
            yamlMapper.writeValue(new File(outputFilePath), camlYaml);

            logger.info("Translation successful! Output written to: {}", outputFilePath);
        } catch (IOException e) {
            logger.error("Error processing YAML files: {}", e.getMessage());
        }
    }

    private static Map<String, Object> translateToscaToCaml(Map<String, Object> toscaYaml, String inputFilePath) {
        Map<String, Object> camlYaml = new LinkedHashMap<>();
        camlYaml.put("apiVersion", "nebulous/v1");
        camlYaml.put("kind", "MetricModel");
        logger.info("Extracting components");
        // Extract components
        List<Map<String, Object>> componentsCaml = new ArrayList<>();

        logger.info("Creating metadata section");
        // Metadata section
        Map<String, Object> metadata = (Map<String, Object>) toscaYaml.get("metadata");
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
        String fileName = new File(inputFilePath).getName();
        metadata.put("fileName", fileName);
        logger.debug("Metadata: {}", metadata);
        camlYaml.put("metadata", metadata);

        Map<String, Object> componentsData = (Map<String, Object>) toscaYaml.get("node_types");
        logger.debug("Component types: {}", componentsData);

        List<String> componentNames = new ArrayList<>();

        if (componentsData != null) {
            for (Map.Entry<String, Object> entry : componentsData.entrySet()) {
                Map<String, Object> componentCaml = new LinkedHashMap<>();
                String componentName = entry.getKey();
                componentCaml.put("name", componentName);
                componentNames.add(componentName);

                Map<String, Object> componentNodeTypes = (Map<String, Object>) entry.getValue();
                
                List<Map<String, Object>> metrics = new ArrayList<>();
                List<Map<String, Object>> requirements = new ArrayList<>();

                // Loop through the node types of the component
                for (Map.Entry<String, Object> nodeType : componentNodeTypes.entrySet()) {
                    Map<String, Object> nodeData = (Map<String, Object>) nodeType.getValue();
                    logger.debug("Node data for {}: {}", entry.getKey(), nodeData);
                    List<Map<String, Object>> capabilities = (List<Map<String, Object>>) nodeData.get("capabilities");
                    logger.debug("Capabilities for {}: {}", entry.getKey(), capabilities);

                    if (capabilities != null) {
                        logger.info("Processing capabilities for {}", entry.getKey());
                        for (Map<String, Object> capability : capabilities) {
                            capability = (Map<String, Object>) capability.entrySet().iterator().next().getValue();

                            logger.debug("{} Capability: {}", capability.get("type"), capability);
                            // search for a capability which has "type" of capabilities.MetricMonitoringCapability
                            if ("capabilities.MetricMonitoringCapability".equals(capability.get("type"))) {
                                Map<String, Object> monitoringProperties = (Map<String, Object>) capability.get("properties");
                                if (monitoringProperties != null) {
                                    // Process raw metrics
                                    List<Map<String, Object>> raw = (List<Map<String, Object>>) monitoringProperties.get("raw");
                                    logger.debug("Raw metrics for {}: {}", entry.getKey(), raw);

                                    if (raw != null) {
                                        for (Map<String, Object> rawMetric : raw) {
                                            Map.Entry<String, Object> rawMetricEntry = (Map.Entry<String, Object>) rawMetric.entrySet().iterator().next();
                                            logger.debug("Raw metric: {}", rawMetric);
                                            Map<String, Object> metric = new LinkedHashMap<>();
                                            metric.put("name", rawMetricEntry.getKey());
                                    
                                            Map<String, Object> rawMetricData = (Map<String, Object>) rawMetricEntry.getValue();
                                            if (rawMetricData != null) {
                                                // Sensor configuration
                                                Map<String, Object> sensor = new LinkedHashMap<>();
                                                sensor.put("type", rawMetricData.get("collector"));
                                                Map<String, Object> sensorConfig = (Map<String, Object>) rawMetricData.get("config");
                                                logger.debug("Sensor config: {}", sensorConfig);
                                                
                                                String collectorInstance = (String) rawMetricData.get("collector_inst");
                                                sensorConfig.put("metric", collectorInstance);
                                                sensor.put("config", sensorConfig);
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
                                    List<Map<String, Object>> composite = (List<Map<String, Object>>) monitoringProperties.get("composite");
                                    logger.debug("Composite metrics for {}: {}", entry.getKey(), composite);

                                    if (composite != null) {
                                        for (Map<String, Object> compositeMetric : composite) {
                                            Map.Entry<String, Object> compositeMetricEntry = (Map.Entry<String, Object>) compositeMetric.entrySet().iterator().next();
                                            Map<String, Object> metric = new LinkedHashMap<>();
                                            metric.put("name", compositeMetricEntry.getKey());
                                            Map<String, Object> compositeData = (Map<String, Object>) compositeMetricEntry.getValue();

                                            if (compositeData != null) {
                                                // Handle the formula
                                                Map<String, Object> formula = (Map<String, Object>) compositeData.get("formula");
                                                if (formula != null) {
                                                    String collectionOutput = (String) formula.get("collection_output");
                                                    String collectionFrequency = (String) formula.get("collection_frequency");
                                                    metric.put("formula", formula.get("type") + "(" + formula.get("argument") + ")");
                                                    metric.put("output", collectionOutput + " " + collectionFrequency);
                                                }

                                                // Handle window
                                                Map<String, Object> window = (Map<String, Object>) compositeData.get("window");
                                                if (window != null) {
                                                    String windowType = (String) window.get("type");
                                                    String windowSize = (String) window.get("size");
                                                    metric.put("window", windowType + " " + windowSize); 
                                                }

                                                // Add output and frequency
                                                Map<String, Object> processing = (Map<String, Object>) compositeData.get("processing");
                                                if (processing != null) {
                                                    String processingType = (String) processing.get("type");
                                                    String processingCriteria = (String) processing.get("criteria");
                                                    metric.put(processingType, processingCriteria);
                                                }

                                                // Add to metrics
                                                metrics.add(metric);

                                            }
                                        }
                                    }
                                }
                            }

                                // Requirements
                            if ("capabilities.SloMonitoringCapability".equals(capability.get("type"))) {
                                List<Map<String, Object>> sloProperties = (List<Map<String, Object>>) capability.get("properties");
                                if (sloProperties != null) {
                                    logger.debug("SLO properties for {}: {}", entry.getKey(), sloProperties);
                                    for (Map<String, Object> slo : sloProperties) {
                                        Map.Entry<String, Object> sloEntry = (Map.Entry<String, Object>) slo.entrySet().iterator().next();
                                        Map<String, Object> sloRequirement = new LinkedHashMap<>();
                                        sloRequirement.put("name", sloEntry.getKey());
                                        sloRequirement.put("type", "slo");
                                        Map<String, Object> sloData = (Map<String, Object>) sloEntry.getValue();
                                        sloRequirement.put("constraint", sloData.get("constraint"));
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
        }

        logger.info("Creating scopes section");
        // Scopes section
        List<Map<String, Object>> scopes = new ArrayList<>();
        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("name", "a_scope");
        scope.put("components", componentNames);
        scopes.add(scope);
        logger.debug("Scopes: {}", scopes);

        //spec contains components and scopes
        camlYaml.put("spec", Map.of("components", componentsCaml, "scopes", scopes));
        logger.debug("Components: {}", componentsCaml);

        return camlYaml;
    }
}

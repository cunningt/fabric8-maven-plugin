/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.maven.enricher.fabric8;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.maven.core.config.ProcessorConfig;
import io.fabric8.maven.core.util.Configs;
import io.fabric8.maven.docker.config.ImageConfiguration;
import io.fabric8.maven.enricher.api.EnricherContext;
import mockit.Expectations;
import mockit.Mocked;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

public class ServiceDiscoveryEnricherTest {

    @Mocked
    private EnricherContext context;
    @Mocked
    ImageConfiguration imageConfiguration;

    private enum Config implements Configs.Key {
        discoverable,
        springDir;
        public String def() { return d; } protected String d;
    }

    // *******************************
    // Tests
    // *******************************

    @Test
    public void testCustomSourceDir() throws Exception {
        String springDir = new File("src/test/resources/spring").getAbsolutePath();
        final ProcessorConfig config = new ProcessorConfig(
            null,
            null,
            Collections.singletonMap(
                ServiceDiscoveryEnricher.ENRICHER_NAME,
                new TreeMap(Collections.singletonMap(
                    Config.springDir.name(),
                    springDir)
                )
            )
        );

        // Setup mock behaviour
        new Expectations() {{
            context.getConfig(); result = config;
        }};

        ServiceDiscoveryEnricher enricher = new ServiceDiscoveryEnricher(context);
        ServicePort servicePort = new ServicePort();
        servicePort.setPort(443);
        List<ServicePort> servicePorts = new ArrayList<>();
        servicePorts.add(servicePort);
        ServiceBuilder serviceBuilder = new ServiceBuilder();
        
        serviceBuilder.editOrNewSpec().addAllToPorts(servicePorts).and().build();
        enricher.addAnnotations(serviceBuilder);
        Map<String, String> annotations = serviceBuilder.buildMetadata().getAnnotations();

        assertEquals(5, annotations.size());
        assertEquals("v1",                 annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.DISCOVERY_VERSION ));
        assertEquals("https",              annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.SCHEME ));
        assertEquals("/myapi",             annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.PATH ));
        assertEquals("443",                annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.PORT ));
        assertEquals("/myapi/openapi.json",annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.DESCRIPTION_PATH ));

        Map<String, String> labels = serviceBuilder.buildMetadata().getLabels();
        assertEquals(1, labels.size());
        assertEquals("true", labels.get(ServiceDiscoveryEnricher.PREFIX));
    }
    
    @Test
    public void testLabelFalse() throws Exception {
        String springDir = new File("src/test/resources/spring").getAbsolutePath();

        TreeMap configMap = new TreeMap();
        configMap.put(Config.springDir.name(), springDir);
        configMap.put(Config.discoverable.name(), "false");

        final ProcessorConfig config = new ProcessorConfig(
            null,
            null,
            Collections.singletonMap(
                    ServiceDiscoveryEnricher.ENRICHER_NAME,
                    configMap)
        );

        // Setup mock behaviour
        new Expectations() {{
            context.getConfig(); result = config;
        }};

        ServiceDiscoveryEnricher enricher = new ServiceDiscoveryEnricher(context);
        ServiceBuilder serviceBuilder = new ServiceBuilder();
        serviceBuilder.editOrNewSpec().and().build();
        enricher.addAnnotations(serviceBuilder);
        
        Map<String, String> annotations = serviceBuilder.buildMetadata().getAnnotations();

        assertEquals(5, annotations.size());
        assertEquals("v1",                 annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.DISCOVERY_VERSION ));
        assertEquals("https",              annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.SCHEME ));
        assertEquals("/myapi",             annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.PATH ));
        assertEquals("80",                 annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.PORT ));
        assertEquals("/myapi/openapi.json",annotations.get(ServiceDiscoveryEnricher.PREFIX + "/" + ServiceDiscoveryEnricher.DESCRIPTION_PATH ));

        Map<String, String> labels = serviceBuilder.buildMetadata().getLabels();
        assertEquals(1, labels.size());
        assertEquals("false", labels.get(ServiceDiscoveryEnricher.PREFIX));
    }


}

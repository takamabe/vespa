package com.yahoo.vespa.config.server.metrics;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.yahoo.config.provision.ApplicationId;
import com.yahoo.vespa.config.server.http.v2.MetricsResponse;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.*;


/**
 * @author olaa
 */
public class MetricsRetrieverTest {

    @Rule
    public final WireMockRule wireMock = new WireMockRule(options().port(8080), true);

    @Test
    public void testMetricAggregation() throws IOException {
        MetricsRetriever metricsRetriever = new MetricsRetriever();

        ApplicationId applicationId = ApplicationId.from("tenant", "app", "default");

        List<ClusterInfo> clusters = List.of(new ClusterInfo("cluster1", ClusterInfo.ClusterType.content, List.of(URI.create("http://localhost:8080/1"), URI.create("http://localhost:8080/2"))),
                new ClusterInfo("cluster2", ClusterInfo.ClusterType.container, List.of(URI.create("http://localhost:8080/3"))));

        Map<ApplicationId, Collection<ClusterInfo>> applications = Map.of(applicationId, clusters);

        stubFor(get(urlEqualTo("/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(contentMetrics())));

        stubFor(get(urlEqualTo("/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(contentMetrics())));

        stubFor(get(urlEqualTo("/3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(containerMetrics())));

        MetricsResponse metricsResponse = metricsRetriever.retrieveAllMetrics(applications);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        metricsResponse.render(bos);
        String expectedResponse = "[\n" +
                " {\n" +
                "  \"applicationId\": \"tenant:app:default\",\n" +
                "  \"clusters\": [\n" +
                "   {\n" +
                "    \"clusterId\": \"cluster1\",\n" +
                "    \"clusterType\": \"content\",\n" +
                "    \"metrics\": {\n" +
                "     \"documentCount\": 6000.0\n" +
                "    }\n" +
                "   },\n" +
                "   {\n" +
                "    \"clusterId\": \"cluster2\",\n" +
                "    \"clusterType\": \"container\",\n" +
                "    \"metrics\": {\n" +
                "     \"queriesPerSecond\": 1.4333333333333333,\n" +
                "     \"feedPerSecond\": 0.7166666666666667,\n" +
                "     \"queryLatency\": 93.02325581395348,\n" +
                "     \"feedLatency\": 69.76744186046511\n" +
                "    }\n" +
                "   }\n" +
                "  ]\n" +
                " }\n" +
                "]\n";
        assertEquals(expectedResponse, bos.toString());
        wireMock.stop();

    }

    private String containerMetrics() throws IOException {
        return Files.readString(Path.of("src/test/resources/metrics/container_metrics"));
    }

    private String contentMetrics() throws IOException {
        return Files.readString(Path.of("src/test/resources/metrics/content_metrics"));
    }
}
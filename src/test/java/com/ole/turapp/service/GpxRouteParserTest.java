package com.ole.turapp.service;

import com.ole.turapp.service.GpxRouteParser.Coordinate;
import com.ole.turapp.service.GpxRouteParser.ParsedRoute;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GpxRouteParserTest {

    private final GpxRouteParser parser = new GpxRouteParser();

    private InputStream gpx(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parse_SingleRoute_ReturnsNameDescriptionAndPoints() {
        String xml = """
                <?xml version="1.0"?>
                <gpx>
                  <rte>
                    <name>Summit trail</name>
                    <desc>A nice hike</desc>
                    <rtept lat="59.9" lon="10.7"></rtept>
                    <rtept lat="59.91" lon="10.71"></rtept>
                  </rte>
                </gpx>
                """;

        List<ParsedRoute> routes = parser.parse(gpx(xml));

        assertThat(routes).hasSize(1);
        ParsedRoute route = routes.get(0);
        assertThat(route.name()).isEqualTo("Summit trail");
        assertThat(route.description()).isEqualTo("A nice hike");
        assertThat(route.points()).containsExactly(
                new Coordinate(59.9, 10.7),
                new Coordinate(59.91, 10.71));
    }

    @Test
    void parse_MultipleRoutes_ReturnsAllInOrder() {
        String xml = """
                <?xml version="1.0"?>
                <gpx>
                  <rte><name>Trail 1</name><rtept lat="1" lon="2"></rtept></rte>
                  <rte><name>Trail 2</name><rtept lat="3" lon="4"></rtept></rte>
                </gpx>
                """;

        List<ParsedRoute> routes = parser.parse(gpx(xml));

        assertThat(routes).extracting(ParsedRoute::name).containsExactly("Trail 1", "Trail 2");
    }

    @Test
    void parse_NoRouteElements_ReturnsEmptyList() {
        assertThat(parser.parse(gpx("<gpx></gpx>"))).isEmpty();
    }

    @Test
    void parse_RouteWithoutNameOrDescription_ReturnsNulls() {
        String xml = """
                <gpx>
                  <rte><rtept lat="1" lon="2"></rtept></rte>
                </gpx>
                """;

        ParsedRoute route = parser.parse(gpx(xml)).get(0);

        assertThat(route.name()).isNull();
        assertThat(route.description()).isNull();
    }

    @Test
    void parse_MissingLatAttribute_ThrowsException() {
        String xml = """
                <gpx>
                  <rte><rtept lon="2"></rtept></rte>
                </gpx>
                """;

        assertThatThrownBy(() -> parser.parse(gpx(xml)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lat");
    }

    @Test
    void parse_NonNumericLatAttribute_ThrowsException() {
        String xml = """
                <gpx>
                  <rte><rtept lat="not-a-number" lon="2"></rtept></rte>
                </gpx>
                """;

        assertThatThrownBy(() -> parser.parse(gpx(xml)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid lat value");
    }

    @Test
    void parse_MalformedXml_ThrowsException() {
        assertThatThrownBy(() -> parser.parse(gpx("<gpx><rte>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not read the GPX file");
    }

    @Test
    void parse_DocumentWithDoctype_RejectedForXxeHardening() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE gpx [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <gpx><rte><rtept lat="1" lon="2"></rtept></rte></gpx>
                """;

        assertThatThrownBy(() -> parser.parse(gpx(xml)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

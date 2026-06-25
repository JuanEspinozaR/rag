package com.ragplatform.infrastructure.connector;

import com.ragplatform.domain.enums.SourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that selects the appropriate DataSourceConnector based on SourceType.
 * New connectors are auto-discovered via Spring's dependency injection.
 */
@Slf4j
@Component
public class ConnectorFactory {

    private final Map<SourceType, DataSourceConnector> connectors;

    public ConnectorFactory(List<DataSourceConnector> connectorList) {
        this.connectors = connectorList.stream()
                .collect(Collectors.toMap(DataSourceConnector::supportedType, Function.identity()));
        log.info("Registered connectors: {}", connectors.keySet());
    }

    public DataSourceConnector getConnector(SourceType type) {
        DataSourceConnector connector = connectors.get(type);
        if (connector == null) {
            throw new UnsupportedOperationException(
                    "No connector registered for source type: " + type);
        }
        return connector;
    }

    public boolean supports(SourceType type) {
        return connectors.containsKey(type);
    }
}

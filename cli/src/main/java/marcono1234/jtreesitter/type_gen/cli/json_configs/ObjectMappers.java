package marcono1234.jtreesitter.type_gen.cli.json_configs;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class ObjectMappers {
    private ObjectMappers() {
    }

    static JsonMapper.Builder verboseMapperBuilder() {
        return JsonMapper.builder()
            // Enhance exceptions for easier troubleshooting; the JSON files are not expected to contain sensitive information
            // which must not be leaked
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            // Fail if user accidentally specified unknown property in JSON config file
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public static final JsonMapper verboseJsonMapper = verboseMapperBuilder().build();
}

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;

public class Json {

    private static ObjectMapper Mapper = thisMapper();

    public static ObjectMapper thisMapper(){
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    public static JsonNode parse(String jsonSrc) throws IOException {
        return Mapper.readTree(jsonSrc);

    }

    public static <A> A fromJson(JsonNode node, Class<A> a) throws JsonProcessingException{
        return Mapper.treeToValue(node, a);
    }

    public static JsonNode toJson(Object obj) {
        return Mapper.valueToTree(obj);
    }

    private static String toString(JsonNode node) throws JsonProcessingException {
        return generateJson(node, false);

    }

    private static String toStringPretty(JsonNode node) throws JsonProcessingException {
        return generateJson(node, true);

    }

    private static String generateJson(Object o, boolean pretty) throws JsonProcessingException {
        ObjectWriter objectWriter = Mapper.writer();
        if (pretty){
            objectWriter = objectWriter.with(SerializationFeature.INDENT_OUTPUT);
        }
        return objectWriter.writeValueAsString(o);
    }

}

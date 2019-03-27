package org.jboss.resteasy.test.tracing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.tracing.api.RESTEasyTracing;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonFormattedTracingInfoTest extends BasicTracingTest {

   private static final Logger LOG = Logger.getLogger(JsonFormattedTracingInfoTest.class);

   @Test
   @OperateOnDeployment(WAR_BASIC_TRACING_FILE)
   public void testJsonTracing() {

      String url = generateURL("/logger", WAR_BASIC_TRACING_FILE);
      WebTarget base = client.target(url);
      try {
         Response response = base.request().header(RESTEasyTracing.HEADER_ACCEPT_FORMAT, "JSON").get();
         LOG.info(response);

         Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
         boolean hasTracing = false;
         for (Map.Entry entry : response.getStringHeaders().entrySet()) {
            if (entry.getKey().toString().startsWith(RESTEasyTracing.HEADER_TRACING_PREFIX)) {
               hasTracing = true;
               String jsonText = entry.getValue().toString();
               ObjectMapper objectMapper = new ObjectMapper();

               TypeReference<List<List<Map<String, String>>>> jsonTraceMsgsListTypeRef =
                     new TypeReference<List<List<Map<String, String>>>>() {};
               List<List<Map>> messageList = objectMapper.readValue(jsonText, jsonTraceMsgsListTypeRef);
               assertNotNull(messageList);
               assertNotNull(messageList.get(0));
               List<Map> list = messageList.get(0);

               String[] keys = {"requestId", "duration", "text", "event", "timestamp"};
               for (Map map : list) {
                  assertNotNull(map);
                  LOG.info("<K, V> ->" + map.toString());


                  for (String key : keys) {
                     assertNotNull(map.get(key));
                  }
               }
               break;
            }
         }
         assertTrue(hasTracing);
         response.close();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}

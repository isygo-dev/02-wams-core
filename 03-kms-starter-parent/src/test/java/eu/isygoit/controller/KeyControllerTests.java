package eu.isygoit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.service.IKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KeyController.class)
public class KeyControllerTests {

    @MockBean
    private IKeyService keyService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void renewKeyByName() throws Exception {
        /*mockMvc.perform(post("/api/v1/private/key").contentType(MediaType.APPLICATION_JSON)
                        .queryParam(RestApiConstants.DOMAIN_NAME, "")
                        .queryParam(RestApiConstants.keyName, "")
                        .queryParam(RestApiConstants.length, "")
                        .queryParam(RestApiConstants.charSetType, "")
                        .content(objectMapper.writeValueAsString(tutorial)))
                .andExpect(status().isCreated())
                .andDo(print());*/
    }
}

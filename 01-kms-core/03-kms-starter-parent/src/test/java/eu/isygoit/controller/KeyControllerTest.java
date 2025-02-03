package eu.isygoit.controller;

import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.RandomKeyNotFoundException;
import eu.isygoit.model.RandomKey;
import eu.isygoit.service.IKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class KeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IKeyService keyService;

    private RequestContextDto requestContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        requestContext = new RequestContextDto(); // Set up any necessary details for the request context
    }

    @Test
    void testGenerateRandomKey() throws Exception {
        String expectedKey = "randomKey123";
        when(keyService.getRandomKey(10, IEnumCharSet.Types.ALPHANUM)).thenReturn(expectedKey);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/private/key/generateRandomKey")
                        .param("length", "10")
                        .param("charSetType", IEnumCharSet.Types.ALPHANUM.name()))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedKey))
                .andReturn();

        verify(keyService, times(1)).getRandomKey(10, IEnumCharSet.Types.ALPHANUM);
    }

    @Test
    void testRenewKeyByName() throws Exception {
        String domain = "testDomain";
        String keyName = "testKeyName";
        String newKey = "renewedKey123";
        when(keyService.getRandomKey(10, IEnumCharSet.Types.ALPHANUM)).thenReturn(newKey);
        doNothing().when(keyService).createOrUpdateKeyByName(domain, keyName, newKey);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/private/key/renewKeyByName")
                        .param("domain", domain)
                        .param("keyName", keyName)
                        .param("length", "10")
                        .param("charSetType", IEnumCharSet.Types.ALPHANUM.name()))
                .andExpect(status().isOk())
                .andExpect(content().string(newKey))
                .andReturn();

        verify(keyService, times(1)).getRandomKey(10, IEnumCharSet.Types.ALPHANUM);
        verify(keyService, times(1)).createOrUpdateKeyByName(domain, keyName, newKey);
    }

    @Test
    void testGetKeyByName_found() throws Exception {
        String domain = "testDomain";
        String keyName = "testKeyName";
        String keyValue = "keyValue123";
        when(keyService.getKeyByName(domain, keyName)).thenReturn(java.util.Optional.of(RandomKey.builder()
                .domain(domain)
                .name("test")
                .value("12132154")
                .build()));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/private/key/getKeyByName")
                        .param("domain", domain)
                        .param("keyName", keyName))
                .andExpect(status().isOk())
                .andExpect(content().string(keyValue))
                .andReturn();

        verify(keyService, times(1)).getKeyByName(domain, keyName);
    }

    @Test
    void testGetKeyByName_notFound() throws Exception {
        String domain = "testDomain";
        String keyName = "testKeyName";
        when(keyService.getKeyByName(domain, keyName)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/private/key/getKeyByName")
                        .param("domain", domain)
                        .param("keyName", keyName))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertInstanceOf(RandomKeyNotFoundException.class, result.getResolvedException()))
                .andReturn();

        verify(keyService, times(1)).getKeyByName(domain, keyName);
    }
}
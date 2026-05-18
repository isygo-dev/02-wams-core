package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequest;
import eu.isygoit.dto.KmsDtos.CustomKeyStoreResponseDto;
import eu.isygoit.dto.KmsDtos.ListCustomKeyStoresResponseDto;
import eu.isygoit.enums.IEnumCustomKeyStoreStatus;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.exception.CustomKeyStoreHasKeysException;
import eu.isygoit.exception.DuplicateCustomKeyStoreNameException;
import eu.isygoit.model.CustomKeyStore;
import eu.isygoit.repository.CustomKeyStoreRepository;
import eu.isygoit.service.IKeyManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomKeyStoreServiceTest {

    private static final String TENANT = "tenant-1";


    @Mock
    private CustomKeyStoreRepository customKeyStoreRepository;

    @Mock
    private IKeyManagementService keyManagementService;

    @InjectMocks
    private CustomKeyStoreService service;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure tenant limit is reasonable for tests
        Field maxField = CustomKeyStoreService.class.getDeclaredField("maxStoresPerTenant");
        maxField.setAccessible(true);
        maxField.set(service, 10);

        Field hbField = CustomKeyStoreService.class.getDeclaredField("connectionHeartbeatSeconds");
        hbField.setAccessible(true);
        hbField.set(service, 60);
    }

    @Test
    void shouldCreateCloudHsmStoreSuccessfully() {
        CreateCustomKeyStoreRequest req = new CreateCustomKeyStoreRequest();
        req.setCustomKeyStoreName("store1");
        req.setCustomKeyStoreType(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);
        req.setCloudHsmClusterId("cluster-1");
        req.setKeyStorePassword("secret");
        req.setTrustAnchorCertificate("cert");

        when(customKeyStoreRepository.countByTenant(TENANT)).thenReturn(0L);
        when(customKeyStoreRepository.existsByTenantAndName(TENANT, "store1")).thenReturn(false);
        when(customKeyStoreRepository.save(any())).thenAnswer(i -> {
            CustomKeyStore s = i.getArgument(0);
            s.setId(1L);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        CustomKeyStoreResponseDto resp = service.createCustomKeyStore(TENANT, req);
        assertNotNull(resp);
        assertEquals(1L, resp.getKeyStoreId());
        assertEquals(IEnumCustomKeyStoreStatus.Types.DISCONNECTED, resp.getStatus());
    }

    @Test
    void shouldThrowOnDuplicateName() {
        CreateCustomKeyStoreRequest req = new CreateCustomKeyStoreRequest();
        req.setCustomKeyStoreName("store1");
        req.setCustomKeyStoreType(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);
        req.setCloudHsmClusterId("cluster-1");
        req.setKeyStorePassword("secret");
        req.setTrustAnchorCertificate("cert");

        when(customKeyStoreRepository.countByTenant(TENANT)).thenReturn(0L);
        when(customKeyStoreRepository.existsByTenantAndName(TENANT, "store1")).thenReturn(true);

        assertThrows(DuplicateCustomKeyStoreNameException.class,
                () -> service.createCustomKeyStore(TENANT, req));
    }

    @Test
    void shouldConnectAndDisconnectCustomKeyStore() {
        CustomKeyStore store = new CustomKeyStore();
        store.setId(2L);
        store.setTenant(TENANT);
        store.setName("store2");
        store.setType(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);
        store.setStatus(IEnumCustomKeyStoreStatus.Types.DISCONNECTED);
        store.setKeyStorePassword("secretHash");

        when(customKeyStoreRepository.findByTenantAndId(TENANT, 2L)).thenReturn(Optional.of(store));
        when(customKeyStoreRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Connect
        service.connectCustomKeyStore(TENANT, 2L);
        assertEquals(IEnumCustomKeyStoreStatus.Types.CONNECTED, store.getStatus());

        // Disconnect
        service.disconnectCustomKeyStore(TENANT, 2L);
        assertEquals(IEnumCustomKeyStoreStatus.Types.DISCONNECTED, store.getStatus());
    }

    @Test
    void shouldPreventDeleteWhenKeysExist() {
        CustomKeyStore store = new CustomKeyStore();
        store.setId(3L);
        store.setTenant(TENANT);
        store.setName("store3");
        store.setStatus(IEnumCustomKeyStoreStatus.Types.DISCONNECTED);

        when(customKeyStoreRepository.findByTenantAndId(TENANT, 3L)).thenReturn(Optional.of(store));
        when(keyManagementService.countKeysInCustomKeyStore(TENANT, 3L)).thenReturn(1);

        assertThrows(CustomKeyStoreHasKeysException.class, () -> service.deleteCustomKeyStore(TENANT, 3L));
    }

    @Test
    void shouldListCustomKeyStoresWithPagination() {
        CustomKeyStore s1 = new CustomKeyStore();
        s1.setId(10L);
        s1.setName("a");
        CustomKeyStore s2 = new CustomKeyStore();
        s2.setId(11L);
        s2.setName("b");

        when(customKeyStoreRepository.findByTenantOrderByIdAsc(TENANT,
                PageRequest.of(0, 2, Sort.by("createDate").descending())))
                .thenReturn(List.of(s1, s2));
        ListCustomKeyStoresResponseDto resp = service.listCustomKeyStores(TENANT, 2, null);
        assertNotNull(resp);
        assertEquals(2, resp.getCustomKeyStores().size());
        assertTrue(resp.isTruncated());
    }
}

package eu.isygoit.remote.ims;

import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.AccountGlobalStatDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.enums.IEnumSharedStatType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "account-stat", path = "/api/v1/private/account/stat")
public interface AccountStatisticsService {

    @GetMapping("/global")
    AccountGlobalStatDto getGlobalStatistics(@RequestParam("statType") IEnumSharedStatType.Types statType);

    @GetMapping("/object")
    AccountStatDto getObjectStatistics(@RequestParam("code") String code);
}
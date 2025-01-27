package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.ApiPermissionDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.exception.ObjectNotFoundException;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.ApiPermissionMapper;
import eu.isygoit.mapper.RoleInfoMapper;
import eu.isygoit.model.DistinctApiPermission;
import eu.isygoit.model.RoleInfo;
import eu.isygoit.repository.ApiPermissionRepository;
import eu.isygoit.service.impl.RoleInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


/**
 * The type Role info controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = ImsExceptionHandler.class, mapper = RoleInfoMapper.class, minMapper = RoleInfoMapper.class, service = RoleInfoService.class)
@RequestMapping(path = "/api/v1/private/roleInfo")
public class RoleInfoController extends MappedCrudController<Long, RoleInfo, RoleInfoDto, RoleInfoDto, RoleInfoService> {

    @Autowired
    private ApiPermissionRepository apiPermissionRepository;
    @Autowired
    private ApiPermissionMapper apiPermissionMapper;

    @Override
    public RoleInfoDto afterFindById(RoleInfoDto roleInfoDto) {
        List<DistinctApiPermission> apiPermissions = apiPermissionRepository.findDistinctServiceNameAndObject();
        Map<String, RolePermissionDto> rolePermissionsMap = new HashMap<>();

        //build all role permission list
        apiPermissions.forEach(apiPermission ->
                createOrUpdateRolePermission(rolePermissionsMap, apiPermission, false, false, false)
        );

        roleInfoDto.getPermissions().forEach(apiPermissionDto -> {
            switch (apiPermissionDto.getRqType()) {
                case GET -> {
                    RolePermissionDto tmpRolePermissionDto = rolePermissionsMap.get(apiPermissionDto.getServiceName() + apiPermissionDto.getObject());
                    tmpRolePermissionDto.setRead(true);
                    rolePermissionsMap.put(apiPermissionDto.getServiceName() + apiPermissionDto.getObject(), tmpRolePermissionDto);
                }
                case POST, PUT -> {
                    RolePermissionDto tmpRolePermissionDto = rolePermissionsMap.get(apiPermissionDto.getServiceName() + apiPermissionDto.getObject());
                    tmpRolePermissionDto.setWrite(true);
                    rolePermissionsMap.put(apiPermissionDto.getServiceName() + apiPermissionDto.getObject(), tmpRolePermissionDto);
                }
                case DELETE -> {
                    RolePermissionDto tmpRolePermissionDto = rolePermissionsMap.get(apiPermissionDto.getServiceName() + apiPermissionDto.getObject());
                    tmpRolePermissionDto.setDelete(true);
                    rolePermissionsMap.put(apiPermissionDto.getServiceName() + apiPermissionDto.getObject(), tmpRolePermissionDto);
                }
            }

        });

        roleInfoDto.setRolePermission(rolePermissionsMap.values().stream().toList());
        return roleInfoDto;
    }

    @Override
    public RoleInfoDto beforeCreate(RoleInfoDto roleInfoDto) {
        if (StringUtils.hasText(roleInfoDto.getTemplateCode())) {
            Optional<RoleInfo> optional = this.crudService().findByCodeIgnoreCase(roleInfoDto.getTemplateCode());
            if (optional.isPresent()) {
                RoleInfoDto copiedRole = mapper().entityToDto(optional.get());
                copiedRole.setId(null);
                copiedRole.setName(roleInfoDto.getName());
                copiedRole.setCode(null);
                copiedRole.setDomain(roleInfoDto.getDomain());
                if (StringUtils.hasText(roleInfoDto.getDescription())) {
                    copiedRole.setDescription(roleInfoDto.getDescription());
                }
                return super.beforeCreate(copiedRole);
            } else {
                throw new ObjectNotFoundException("Role info template with code " + roleInfoDto.getTemplateCode());
            }
        } else {
            List<ApiPermissionDto> apiPermissionDtos = getApiPermissions(roleInfoDto);
            roleInfoDto.setPermissions(apiPermissionDtos);
        }
        return super.beforeCreate(roleInfoDto);
    }

    @Override
    public RoleInfoDto beforeUpdate(Long id, RoleInfoDto roleInfoDto) {
        List<ApiPermissionDto> apiPermissionDtos = getApiPermissions(roleInfoDto);
        roleInfoDto.setPermissions(apiPermissionDtos);
        return super.beforeUpdate(id, roleInfoDto);
    }

    private List<ApiPermissionDto> getApiPermissions(RoleInfoDto roleInfoDto) {
        List<ApiPermissionDto> apiPermissionDtos = new ArrayList<>();
        roleInfoDto.getRolePermission().forEach(rolePermissionDto -> {
            apiPermissionDtos.addAll(apiPermissionMapper.listEntityToDto(apiPermissionRepository.
                    findAllByServiceNameAndObjectAndRqTypeIn(rolePermissionDto.getServiceName(), rolePermissionDto.getObjectName(), rolePermissionDto.getHTTPRequest())));
        });
        return apiPermissionDtos;
    }

    private void createOrUpdateRolePermission(Map<String, RolePermissionDto> rolePermissionDtos, DistinctApiPermission apiPermission, Boolean canRead, Boolean canWrite, Boolean canDelete) {
        if (rolePermissionDtos.containsKey(apiPermission.getServiceName() + apiPermission.getObject())) {
            RolePermissionDto rolePermission = rolePermissionDtos.get(apiPermission.getServiceName() + apiPermission.getObject());
            if (Objects.nonNull(canRead)) rolePermission.setRead(canRead);
            if (Objects.nonNull(canWrite)) rolePermission.setWrite(canWrite);
            if (Objects.nonNull(canDelete)) rolePermission.setDelete(canDelete);
        } else {
            RolePermissionDto rolePermission = RolePermissionDto.builder()
                    .serviceName(apiPermission.getServiceName())
                    .objectName(apiPermission.getObject())
                    .build();
            if (Objects.nonNull(canRead)) rolePermission.setRead(canRead);
            if (Objects.nonNull(canWrite)) rolePermission.setWrite(canWrite);
            if (Objects.nonNull(canDelete)) rolePermission.setDelete(canDelete);
            rolePermissionDtos.put(apiPermission.getServiceName() + apiPermission.getObject()
                    , rolePermission);
        }
    }

    @Override
    public List<RoleInfoDto> afterFindAllFull(RequestContextDto requestContext, List<RoleInfoDto> list) {
        if (!CollectionUtils.isEmpty(list)) {
            //filter roles
            list = crudService().filterNotAllowedRoles(requestContext, list);
        }
        return super.afterFindAllFull(requestContext, list);
    }

    @Override
    public List<RoleInfoDto> afterFindAll(RequestContextDto requestContext, List<RoleInfoDto> list) {
        if (!CollectionUtils.isEmpty(list)) {
            //filter roles
            list = crudService().filterNotAllowedRoles(requestContext, list);
        }
        return super.afterFindAll(requestContext, list);
    }
}

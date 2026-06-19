package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PublicPasswordServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.UserContextRequestDto;
import eu.isygoit.dto.request.GeneratePwdRequestDto;
import eu.isygoit.dto.request.IsPwdExpiredRequestDto;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.IPasswordService;
import eu.isygoit.service.ITokenBuilderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * The type Public password controller.
 */
@Slf4j
@Validated
@Tag(name = "Public Password", description = "Endpoints for public password operations")
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public/password")
public class PublicPasswordController extends ControllerExceptionHandler implements PublicPasswordServiceApi {

    @Autowired
    private ITokenBuilderService tokenService;

    @Autowired
    private IPasswordService passwordService;

    @Override
    public ResponseEntity<Boolean> isOtpExpired(
            IsPwdExpiredRequestDto isPwdExpiredRequestDto) {
        log.info("Call isPasswordExpired {}", isPwdExpiredRequestDto);
        try {
            return ResponseFactory.responseOk(passwordService.isExpired(isPwdExpiredRequestDto.getTenant().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getEmail().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getUserName().trim().toLowerCase()
                    , IEnumAuth.Types.OTP));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isQrcExpired(
            IsPwdExpiredRequestDto isPwdExpiredRequestDto) {
        log.info("Call isPasswordExpired {}", isPwdExpiredRequestDto);
        try {
            return ResponseFactory.responseOk(passwordService.isExpired(isPwdExpiredRequestDto.getTenant().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getEmail().trim().toLowerCase()
                    , isPwdExpiredRequestDto.getUserName().trim().toLowerCase()
                    , IEnumAuth.Types.QRC));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Integer> generateOtp(
            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for tenant {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    generatePwdRequest.getTenant()
                    , generatePwdRequest.getTenantUrl()
                    , generatePwdRequest.getEmail()
                    , generatePwdRequest.getUserName()
                    , generatePwdRequest.getFullName()
                    , IEnumAuth.Types.OTP);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getTenant(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Integer> generateQrc(
            GeneratePwdRequestDto generatePwdRequest) {
        log.info("Call generate password for tenant {}", generatePwdRequest);
        try {
            AccessKeyResponseDto accessKeyResponse = passwordService.generateRandomPassword(
                    generatePwdRequest.getTenant()
                    , generatePwdRequest.getTenantUrl()
                    , generatePwdRequest.getEmail()
                    , generatePwdRequest.getUserName()
                    , generatePwdRequest.getFullName()
                    , IEnumAuth.Types.QRC);
            //Never return the password
            log.info("password generated for {}/{} : {}", generatePwdRequest.getTenant(), generatePwdRequest.getUserName(), accessKeyResponse.getKey());
            return ResponseFactory.responseOk(accessKeyResponse.getLength());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> generateForgotPasswordAccessToken(UserContextRequestDto userContextDto) {
        log.info("Call generateForgotPasswordAccessToken " + userContextDto.toString());
        try {
            tokenService.buildForgotPasswordAccessToken(userContextDto.getTenant(),
                    Set.of(userContextDto.getApplication()),
                    userContextDto.getUserName());
            return ResponseFactory.responseOk(true);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

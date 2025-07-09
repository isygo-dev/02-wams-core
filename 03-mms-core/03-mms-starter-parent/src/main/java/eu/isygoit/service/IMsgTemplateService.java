package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.com.rest.service.IFileServiceMethods;
import eu.isygoit.com.rest.service.tenancy.IFileTenantServiceMethods;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.model.MsgTemplate;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.util.Map;


/**
 * The interface Template service.
 */
public interface IMsgTemplateService extends ICrudTenantServiceMethods<Long, MsgTemplate>,
        IFileTenantServiceMethods<Long, MsgTemplate> {

    /**
     * Compose message body string.
     *
     * @param senderDomainName the sender tenant name
     * @param templateName     the template name
     * @param variables        the variables
     * @return the string
     * @throws IOException       the io exception
     * @throws TemplateException the template exception
     */
    String composeMessageBody(String senderDomainName,
                              IEnumEmailTemplate.Types templateName,
                              Map<String, String> variables) throws IOException, TemplateException;
}

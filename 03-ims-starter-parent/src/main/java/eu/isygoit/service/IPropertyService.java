package eu.isygoit.service;


import eu.isygoit.model.Property;

import java.util.List;


/**
 * The interface Property service.
 */
public interface IPropertyService {

    /**
     * Update property property.
     *
     * @param accountCode the account code
     * @param property    the property
     * @return the property
     */
    Property updateProperty(String accountCode, Property property);


    /**
     * Gets property by account.
     *
     * @param accountCode the account code
     * @param guiName     the gui name
     * @param name        the name
     * @return the property by account
     */
    Property getPropertyByAccount(String accountCode, String guiName, String name);

    /**
     * Gets property by account and gui.
     *
     * @param accountCode the account code
     * @param guiName     the gui name
     * @return the property by account and gui
     */
    List<Property> getPropertyByAccountAndGui(String accountCode, String guiName);

}

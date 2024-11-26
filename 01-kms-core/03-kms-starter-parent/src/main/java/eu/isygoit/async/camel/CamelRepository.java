package eu.isygoit.async.camel;

import eu.isygoit.com.camel.repository.AbstractCamelRepository;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * The type Camel repository.
 */
@Repository
public class CamelRepository extends AbstractCamelRepository {


    /**
     * Instantiates a new Camel repository.
     *
     * @param camelContext the camel context
     */
    protected CamelRepository(@Autowired CamelContext camelContext) {
        super(camelContext);
    }
}

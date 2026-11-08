package eu.isygoit.async.camel;

import eu.isygoit.com.camel.repository.AbstractCamelRepository;
import org.apache.camel.CamelContext;
import org.springframework.stereotype.Repository;

@Repository
public class CamelRepository extends AbstractCamelRepository {
    public CamelRepository(CamelContext camelContext) {
        super(camelContext);
    }
}

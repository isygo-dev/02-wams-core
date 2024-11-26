package eu.isygoit.repository;

import eu.isygoit.enums.IEnumRequest;
import eu.isygoit.model.ApiPermission;
import eu.isygoit.model.DistinctApiPermission;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * The interface Api permission repository.
 */
public interface ApiPermissionRepository extends JpaPagingAndSortingRepository<ApiPermission, Long> {

    /**
     * Find by service name and object and method and rq type and path optional.
     *
     * @param serviceName the service name
     * @param object      the object
     * @param method      the method
     * @param rqType      the rq type
     * @param path        the path
     * @return the optional
     */
    Optional<ApiPermission> findByServiceNameAndObjectAndMethodAndRqTypeAndPath(String serviceName, String object, String method, IEnumRequest.Types rqType, String path);

    /**
     * Find distinct service name and object list.
     *
     * @return the list
     */
    @Query("select distinct new eu.isygoit.model.DistinctApiPermission(a.serviceName, a.object) from ApiPermission a")
    List<DistinctApiPermission> findDistinctServiceNameAndObject();

    /**
     * Find all by service name and object and rq type in list.
     *
     * @param serviceName the service name
     * @param objectName  the object name
     * @param rqType      the rq type
     * @return the list
     */
    List<ApiPermission> findAllByServiceNameAndObjectAndRqTypeIn(String serviceName, String objectName, List<IEnumRequest.Types> rqType);
}

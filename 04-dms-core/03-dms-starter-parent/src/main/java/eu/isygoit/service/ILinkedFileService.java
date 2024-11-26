package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.model.LinkedFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * The interface Linked file service.
 */
public interface ILinkedFileService extends ICrudServiceMethod<Long, LinkedFile> {
    /**
     * Upload string.
     *
     * @param linkedFile the linked file
     * @param file       the file
     * @return the string
     * @throws IOException the io exception
     */
    String upload(LinkedFileRequestDto linkedFile, MultipartFile file) throws IOException;

    /**
     * Search by tags list.
     *
     * @param domain the domain
     * @param tags   the tags
     * @return the list
     * @throws IOException the io exception
     */
    List<LinkedFile> searchByTags(String domain, String tags) throws IOException;

    /**
     * Delete file.
     *
     * @param domain the domain
     * @param code   the code
     * @throws IOException the io exception
     */
    void deleteFile(String domain, String code) throws IOException;

    /**
     * Search by original name linked file.
     *
     * @param domain           the domain
     * @param originalFileName the original name
     * @return the linked file
     * @throws IOException the io exception
     */
    LinkedFile searchByOriginalFileName(String domain, String originalFileName) throws IOException;

    /**
     * Rename file linked file.
     *
     * @param domain  the domain
     * @param code    the old name
     * @param newName the new name
     * @return the linked file
     * @throws IOException the io exception
     */
    LinkedFile renameFile(String domain, String code, String newName) throws IOException;


    /**
     * Search by categories list.
     *
     * @param domain     the domain
     * @param categories the categories
     * @return the list
     * @throws IOException the io exception
     */
    List<LinkedFile> searchByCategories(String domain, List<String> categories) throws IOException;

    /**
     * Download resource.
     *
     * @param originalFileName the original file name
     * @param domain           the domain
     * @param version          the version
     * @return the resource
     * @throws IOException the io exception
     */
    Resource download(String domain, String code) throws IOException;
}

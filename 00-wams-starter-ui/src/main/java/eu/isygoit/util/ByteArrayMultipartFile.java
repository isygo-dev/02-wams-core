package eu.isygoit.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Minimal {@link MultipartFile} adapter over an in-memory byte array.
 * <p>
 * Vaadin's {@code Upload} component (backed by {@code MemoryBuffer} or
 * similar receivers) only ever hands back an {@link InputStream} — the file
 * never arrives as part of a real Spring MVC multipart HTTP request, so no
 * genuine {@code MultipartFile} exists to pass along to APIs that expect
 * one (e.g. file-upload request DTOs). This wraps already-read bytes to
 * bridge that gap.
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String name;
    private final String contentType;

    public ByteArrayMultipartFile(byte[] content, String name, String contentType) {
        this.content = content;
        this.name = name;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return name;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), content);
    }
}

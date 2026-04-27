package com.yas.media.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit tests for {@link FileTypeValidator}.
 *
 * <p>Covers: null file, null content-type, disallowed type, allowed type with
 * valid image bytes, and allowed type with non-image bytes.
 */
@ExtendWith(MockitoExtension.class)
class FileTypeValidatorTest {

    private FileTypeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        // Build a real ValidFileType annotation stub so we can call initialize()
        ValidFileType annotation = mock(ValidFileType.class);
        when(annotation.allowedTypes()).thenReturn(new String[]{"image/jpeg", "image/png", "image/gif"});
        when(annotation.message()).thenReturn("File type not allowed");

        validator = new FileTypeValidator();
        validator.initialize(annotation);

        // Default context wiring for the "invalid" path
        when(context.buildConstraintViolationWithTemplate("File type not allowed"))
            .thenReturn(violationBuilder);
    }

    @Nested
    class IsValidTest {

        @Test
        void testIsValid_whenFileIsNull_shouldReturnFalse() {
            boolean result = validator.isValid(null, context);
            assertThat(result).isFalse();
        }

        @Test
        void testIsValid_whenContentTypeIsNull_shouldReturnFalse() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getContentType()).thenReturn(null);

            boolean result = validator.isValid(file, context);
            assertThat(result).isFalse();
        }

        @Test
        void testIsValid_whenContentTypeNotAllowed_shouldReturnFalse() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getContentType()).thenReturn("application/pdf");

            boolean result = validator.isValid(file, context);
            assertThat(result).isFalse();
        }

        @Test
        void testIsValid_whenAllowedTypeButNotRealImage_shouldReturnFalse() throws IOException {
            // Provide bytes that are NOT a valid image (empty byte array → ImageIO returns null)
            MultipartFile file = mock(MultipartFile.class);
            when(file.getContentType()).thenReturn("image/png");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

            boolean result = validator.isValid(file, context);
            assertThat(result).isFalse();
        }

        @Test
        void testIsValid_whenGetInputStreamThrowsIOException_shouldReturnFalse() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getContentType()).thenReturn("image/jpeg");
            InputStream brokenStream = mock(InputStream.class);
            when(file.getInputStream()).thenReturn(brokenStream);
            // Make the stream throw when ImageIO tries to read it
            when(brokenStream.read()).thenThrow(new IOException("broken"));
            when(brokenStream.read(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenThrow(new IOException("broken"));

            boolean result = validator.isValid(file, context);
            assertThat(result).isFalse();
        }
    }
}

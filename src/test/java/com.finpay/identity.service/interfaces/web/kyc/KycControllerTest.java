package com.finpay.identity.service.interfaces.web.kyc;

import com.finpay.identity.service.application.kyc.KycExtractUseCase;
import com.finpay.identity.service.application.kyc.KycExtractionResult;
import com.finpay.identity.service.domain.kyc.ExtractedKycFields;
import com.finpay.identity.service.domain.kyc.KycDocument;
import com.finpay.identity.service.domain.kyc.KycState;
import com.finpay.identity.service.domain.kyc.KycValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KycControllerTest {

    @Test
    void post_extract_delegates_to_use_case_and_reports_review() {
        KycExtractUseCase useCase = mock(KycExtractUseCase.class);
        KycController controller = new KycController(useCase);
        UUID customerId = UUID.randomUUID();
        when(useCase.extract(eq(customerId), any(KycDocument.class))).thenReturn(new KycExtractionResult(
                UUID.randomUUID(), customerId, KycState.REVIEW,
                new ExtractedKycFields("PASSPORT", "Jane Doe", LocalDate.of(1990, 1, 1), "AB123456")));

        KycExtractionResponse response = controller.extract(customerId, file());

        assertThat(response.kycState()).isEqualTo("REVIEW");
        assertThat(response.extractedFields().fullName()).isEqualTo("Jane Doe");
        assertThat(response.message()).contains("REVIEW");
        verify(useCase).extract(eq(customerId), any(KycDocument.class));
    }

    @Test
    void empty_upload_is_rejected_before_reaching_the_use_case() {
        KycExtractUseCase useCase = mock(KycExtractUseCase.class);
        KycController controller = new KycController(useCase);

        assertThatThrownBy(() -> controller.extract(UUID.randomUUID(), emptyFile()))
                .isInstanceOf(KycValidationException.class)
                .hasMessageContaining("must not be empty");
        verifyNoInteractions(useCase);
    }

    private MultipartFile file() {
        return simpleFile(false);
    }

    private MultipartFile emptyFile() {
        return simpleFile(true);
    }

    private MultipartFile simpleFile(boolean empty) {
        byte[] content = empty ? new byte[0] : new byte[]{1, 2, 3};
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return "passport.png";
            }

            @Override
            public String getContentType() {
                return "image/png";
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
            public void transferTo(File dest) throws IOException, IllegalStateException {
                Files.write(dest.toPath(), content);
            }

            @Override
            public void transferTo(Path dest) throws IOException {
                Files.write(dest, content);
            }
        };
    }
}

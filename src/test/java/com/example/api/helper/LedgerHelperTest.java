package com.example.api.helper;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class LedgerHelperTest {
    @Test
    void Given_LedgerError_With_ExpectedFormat_When_ProcessingErrorCode_Then_CodeReturned() {
        String testError = "StatusRuntimeException: testCode: Command";
        Assertions.assertEquals("testCode", LedgerHelper.extractErrorCode(testError));
    }

    @Test
    void Given_MalformedLedgerError_When_ProcessingErrorCode_Then_Return400() {
        Assertions.assertEquals("BAD_REQUEST", LedgerHelper.extractErrorCode(RandomStringUtils.randomAlphabetic(8)));
    }

    @Test
    void Given_LedgerError_With_ExpectedFormat_When_ProcessingErrorMessage_Then_MessageReturned() {
        String testError = "Error: testError Details:";
        Assertions.assertEquals("testError ", LedgerHelper.extractErrorMessage(testError));
    }

    @Test
    void Given_MalformedLedgerError_When_ProcessingErrorMessage_Then_FullErrorReturned() {
        String randomErrorTest = RandomStringUtils.randomAlphabetic(8);
        Assertions.assertEquals(randomErrorTest, LedgerHelper.extractErrorMessage(randomErrorTest));
    }
}

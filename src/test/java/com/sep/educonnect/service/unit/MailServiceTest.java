package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import com.sep.educonnect.dto.mail.Attachment;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.mail.Mailer;
import com.sep.educonnect.service.email.MailService;
import com.sep.educonnect.service.email.ThymeleafEngine;
import com.sep.educonnect.utils.FileUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailService Unit Tests")
class MailServiceTest {

    @Mock private MailgunMessagesApi mailgunMessagesApi;

    @Mock private ThymeleafEngine thymeleafEngine;

    @InjectMocks private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "domain", "mg.example.com");
        ReflectionTestUtils.setField(mailService, "fromEmail", "no-reply@example.com");
        ReflectionTestUtils.setField(mailService, "fromName", "EduConnect");
        when(thymeleafEngine.renderHtmlMail(anyMap(), anyString()))
                .thenReturn("<html>content</html>");
    }

    @Test
    @DisplayName("Should send email using Mailgun")
    void should_sendEmailUsingMailgun() {
        // Given
        Email email =
                Email.builder()
                        .subject("Hello")
                        .to(List.of(Mailer.builder().email("user@example.com").build()))
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.send(email, "template", Map.of("name", "User"));

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), captor.capture());
        Message sentMessage = captor.getValue();
        assertEquals("Hello", sentMessage.getSubject());
        // Mailgun may convert List to Set, so compare contents instead of type
        assertNotNull(sentMessage.getTo());
        assertEquals(1, sentMessage.getTo().size());
        assertTrue(sentMessage.getTo().contains("user@example.com"));
        assertEquals("EduConnect <no-reply@example.com>", sentMessage.getFrom());
    }

    @Test
    @DisplayName("Should skip sending when recipients missing")
    void should_skipWhenRecipientsMissing() {
        // Given
        Email email = Email.builder().subject("No recipients").build();

        // When
        mailService.send(email, "template", Map.of());

        // Then
        verify(mailgunMessagesApi, never()).sendMessage(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Should delegate to send with test template")
    void should_sendTestEmail() {
        // Given
        Email email =
                Email.builder()
                        .subject("Test email")
                        .to(List.of(Mailer.builder().email("user@example.com").build()))
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.sendTestEmail(email, Map.of());

        // Then
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), any(Message.class));
    }

    @Test
    @DisplayName("Should send email with CC recipients")
    void should_sendEmailWithCC() {
        // Given
        Email email =
                Email.builder()
                        .subject("Email with CC")
                        .to(List.of(Mailer.builder().email("user@example.com").build()))
                        .cc(
                                List.of(
                                        Mailer.builder().email("cc1@example.com").build(),
                                        Mailer.builder().email("cc2@example.com").build()))
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.send(email, "template", Map.of());

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), captor.capture());
        Message sentMessage = captor.getValue();
        assertNotNull(sentMessage.getCc());
        assertEquals(2, sentMessage.getCc().size());
        assertTrue(sentMessage.getCc().contains("cc1@example.com"));
        assertTrue(sentMessage.getCc().contains("cc2@example.com"));
    }

    @Test
    @DisplayName("Should send email with BCC recipients")
    void should_sendEmailWithBCC() {
        // Given
        Email email =
                Email.builder()
                        .subject("Email with BCC")
                        .to(List.of(Mailer.builder().email("user@example.com").build()))
                        .bcc(
                                List.of(
                                        Mailer.builder().email("bcc1@example.com").build(),
                                        Mailer.builder().email("bcc2@example.com").build()))
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.send(email, "template", Map.of());

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), captor.capture());
        Message sentMessage = captor.getValue();
        assertNotNull(sentMessage.getBcc());
        assertEquals(2, sentMessage.getBcc().size());
        assertTrue(sentMessage.getBcc().contains("bcc1@example.com"));
        assertTrue(sentMessage.getBcc().contains("bcc2@example.com"));
    }

    @Test
    @DisplayName("Should send email with text part")
    void should_sendEmailWithTextPart() {
        // Given
        Email email =
                Email.builder()
                        .subject("Email with text")
                        .to(List.of(Mailer.builder().email("user@example.com").build()))
                        .textPart("Plain text content")
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.send(email, "template", Map.of());

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), captor.capture());
        Message sentMessage = captor.getValue();
        assertEquals("Plain text content", sentMessage.getText());
    }

    @Test
    @DisplayName("Should send email with HTML part")
    void should_sendEmailWithHtmlPart() {
        // Given
        Email email =
                Email.builder()
                        .subject("Email with HTML")
                        .to(List.of(Mailer.builder().email("user@example.com").build()))
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.send(email, "template", Map.of());

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), captor.capture());
        Message sentMessage = captor.getValue();
        assertEquals("<html>content</html>", sentMessage.getHtml());
    }

    @Test
    @DisplayName("Should send email with multiple TO recipients")
    void should_sendEmailWithMultipleRecipients() {
        // Given
        Email email =
                Email.builder()
                        .subject("Multiple recipients")
                        .to(
                                List.of(
                                        Mailer.builder().email("user1@example.com").build(),
                                        Mailer.builder().email("user2@example.com").build(),
                                        Mailer.builder().email("user3@example.com").build()))
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.send(email, "template", Map.of());

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), captor.capture());
        Message sentMessage = captor.getValue();
        assertEquals(3, sentMessage.getTo().size());
        assertTrue(sentMessage.getTo().contains("user1@example.com"));
        assertTrue(sentMessage.getTo().contains("user2@example.com"));
        assertTrue(sentMessage.getTo().contains("user3@example.com"));
    }

    @Test
    @DisplayName("Should skip sending when TO list is null")
    void should_skipWhenToListIsNull() {
        // Given
        Email email = Email.builder().subject("No TO recipients").to(null).build();

        // When
        mailService.send(email, "template", Map.of());

        // Then
        verify(mailgunMessagesApi, never()).sendMessage(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Should skip sending when TO list is empty")
    void should_skipWhenToListIsEmpty() {
        // Given
        Email email = Email.builder().subject("Empty TO list").to(List.of()).build();

        // When
        mailService.send(email, "template", Map.of());

        // Then
        verify(mailgunMessagesApi, never()).sendMessage(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Should handle exception from Mailgun API")
    void should_handleExceptionFromMailgunAPI() {
        // Given
        Email email =
                Email.builder()
                        .subject("Test")
                        .to(List.of(Mailer.builder().email("user@example.com").build()))
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("Mailgun API error"));

        // When & Then - Should not throw exception, just log error
        assertDoesNotThrow(() -> mailService.send(email, "template", Map.of()));
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), any(Message.class));
    }

    @Test
    @DisplayName("Should send email with all fields populated")
    void should_sendEmailWithAllFields() {
        // Given
        Email email =
                Email.builder()
                        .subject("Complete email")
                        .to(List.of(Mailer.builder().email("to@example.com").build()))
                        .cc(List.of(Mailer.builder().email("cc@example.com").build()))
                        .bcc(List.of(Mailer.builder().email("bcc@example.com").build()))
                        .textPart("Text content")
                        .build();

        when(mailgunMessagesApi.sendMessage(anyString(), any(Message.class)))
                .thenReturn(MessageResponse.builder().id("123").message("Queued").build());

        // When
        mailService.send(email, "template", Map.of("key", "value"));

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mailgunMessagesApi).sendMessage(eq("mg.example.com"), captor.capture());
        Message sentMessage = captor.getValue();
        assertEquals("Complete email", sentMessage.getSubject());
        assertEquals(1, sentMessage.getTo().size());
        assertEquals(1, sentMessage.getCc().size());
        assertEquals(1, sentMessage.getBcc().size());
        assertEquals("Text content", sentMessage.getText());
        assertNotNull(sentMessage.getHtml());
    }

}

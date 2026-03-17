package com.sep.educonnect.service.email;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import com.sep.educonnect.constant.TemplateMail;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.mail.Mailer;
import com.sep.educonnect.utils.FileUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MailService {
    MailgunMessagesApi mailgunMessagesApi;
    ThymeleafEngine thymeleafEngine;

    @Value("${mailgun.domain}")
    @NonFinal
    String domain;

    @Value("${mailgun.from}")
    @NonFinal
    String fromEmail;

    @Value("${mailgun.name}")
    @NonFinal
    String fromName;

    public void send(Email email, String template, Map<String, Object> variables) {
        String html = thymeleafEngine.renderHtmlMail(variables, template);
        email.setHtmlPart(html);

        Message.MessageBuilder messageBuilder = Message.builder()
                .from(StringUtils.isNotBlank(fromName) ? fromName + " <" + fromEmail + ">" : fromEmail)
                .subject(email.getSubject());

        if (email.getTo() != null && !email.getTo().isEmpty()) {
            messageBuilder.to(email.getTo().stream().map(Mailer::getEmail).toList());
        } else {
            log.error("No recipients specified in 'to' field");
            return;
        }

        if (email.getCc() != null && !email.getCc().isEmpty()) {
            messageBuilder.cc(email.getCc().stream().map(Mailer::getEmail).toList());
        }

        if (email.getBcc() != null && !email.getBcc().isEmpty()) {
            messageBuilder.bcc(email.getBcc().stream().map(Mailer::getEmail).toList());
        }

        if (email.getTextPart() != null) {
            messageBuilder.text(email.getTextPart());
        }

        if (email.getHtmlPart() != null) {
            messageBuilder.html(email.getHtmlPart());
        }

        if (email.getAttachments() != null) {
            email.getAttachments().forEach(attachment -> {
                try {
                    File file = FileUtil.decodeBase64ToFile(attachment.getBase64Content(), attachment.getFileName());
                    messageBuilder.attachment(file);
                } catch (IOException e) {
                    log.error("Error decoding attachment: {}", e.getMessage());
                }
            });
        }

        Message message = messageBuilder.build();

        try {
            log.info("Sending email to: {}", email.getTo());
            MessageResponse response = mailgunMessagesApi.sendMessage(domain, message);
            log.info("Mailgun response: {}", response);
        } catch (Exception e) {
            log.error("Error sending email via Mailgun: {}", e.getMessage(), e);
        }
    }

    public void sendTestEmail(Email email, Map<String, Object> variables) {
        send(email, TemplateMail.TEST_EMAIL, variables);
    }

}
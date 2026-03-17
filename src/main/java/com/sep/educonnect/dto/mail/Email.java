package com.sep.educonnect.dto.mail;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
public class Email {
    private Mailer from;
    private List<Mailer> to;
    private List<Mailer> cc;
    private List<Mailer> bcc;
    private String subject;
    private String textPart;
    private String htmlPart;
    @ToString.Exclude
    private List<Attachment> attachments;
}

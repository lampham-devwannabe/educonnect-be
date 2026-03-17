package com.sep.educonnect.dto.mail;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Attachment {
    private String fileName;
    private String base64Content;
}

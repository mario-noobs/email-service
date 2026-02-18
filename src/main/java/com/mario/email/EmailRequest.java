package com.mario.email;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class EmailRequest {

    private final String to;

    @Singular("cc")
    private final List<String> cc;

    @Singular("bcc")
    private final List<String> bcc;

    private final String subject;

    private final String templateName;

    @Singular("model")
    private final Map<String, Object> templateModel;

    @Singular("header")
    private final Map<String, String> headers;

    private final String fromAddress;
}

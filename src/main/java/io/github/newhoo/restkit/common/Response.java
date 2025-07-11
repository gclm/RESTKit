package io.github.newhoo.restkit.common;

import lombok.Data;

/**
 * Response
 *
 * @author newhoo
 * @since 2.0.0
 */
@NotProguard
@Data
public class Response {

    /** response body */
    private String body;
}
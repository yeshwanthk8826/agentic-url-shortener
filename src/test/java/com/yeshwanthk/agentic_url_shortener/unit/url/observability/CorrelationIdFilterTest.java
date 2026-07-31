package com.yeshwanthk.agentic_url_shortener.unit.url.observability;

import com.yeshwanthk.agentic_url_shortener.observability.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter =
            new CorrelationIdFilter();

    @Test
    void preservesValidClientCorrelationId() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var valueInsideChain = new AtomicReference<String>();

        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                "interview-test-123"
        );

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        valueInsideChain.set(
                                MDC.get(CorrelationIdFilter.MDC_KEY)
                        )
        );

        assertThat(response.getHeader(
                CorrelationIdFilter.HEADER_NAME
        )).isEqualTo("interview-test-123");

        assertThat(valueInsideChain)
                .hasValue("interview-test-123");

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                .isNull();
    }

    @Test
    void replacesInvalidClientCorrelationId() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                "invalid value with spaces"
        );

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> {
                }
        );

        String generated = response.getHeader(
                CorrelationIdFilter.HEADER_NAME
        );

        assertThat(generated)
                .isNotBlank()
                .isNotEqualTo("invalid value with spaces")
                .matches(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                                + "[0-9a-f]{4}-[0-9a-f]{12}$"
                );
    }
}

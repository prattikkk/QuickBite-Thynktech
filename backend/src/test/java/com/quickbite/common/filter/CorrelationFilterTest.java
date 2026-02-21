package com.quickbite.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CorrelationFilter.
 */
@ExtendWith(MockitoExtension.class)
class CorrelationFilterTest {

    private final CorrelationFilter filter = new CorrelationFilter();

    @Test
    void generatesRequestId_whenHeaderAbsent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn(null);

        // Capture the MDC value inside the chain
        doAnswer(invocation -> {
            String requestId = MDC.get("requestId");
            assertThat(requestId).isNotNull().isNotEmpty();
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Request-Id"), anyString());
        verify(chain).doFilter(request, response);

        // MDC should be cleaned up after filter completes
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void usesProvidedRequestId_whenHeaderPresent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn("my-custom-id-123");

        doAnswer(invocation -> {
            assertThat(MDC.get("requestId")).isEqualTo("my-custom-id-123");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Request-Id", "my-custom-id-123");
    }

    @Test
    void cleansUpMdc_evenOnException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn(null);
        doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(RuntimeException.class);

        // MDC should be cleaned up
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void blankHeader_generatesNewId() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn("   ");

        doAnswer(invocation -> {
            String requestId = MDC.get("requestId");
            assertThat(requestId).isNotNull().isNotEqualTo("   ");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);
    }
}

package com.primecrm.api.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.shared.util.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void cleanUp() {
        MDC.clear();
        TenantContext.clear();
    }

    @Test
    void doFilter_populatesMdcDuringChainExecutionAndClearsItAfterwards() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String[] capturedRequestId = new String[1];
        String[] capturedTenantId = new String[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedRequestId[0] = MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID);
            capturedTenantId[0] = MDC.get(RequestCorrelationFilter.MDC_TENANT_ID);
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(capturedRequestId[0]).isNotBlank();
        assertThat(capturedTenantId[0]).isEqualTo(TenantContext.getCurrentTenant().toString());
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID)).isNull();
        verify(response).setHeader(eq(RequestCorrelationFilter.REQUEST_ID_HEADER), eq(capturedRequestId[0]));
    }

    @Test
    void doFilter_reusesIncomingRequestIdHeaderInsteadOfGeneratingANewOne() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).thenReturn("client-request-id");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "client-request-id");
    }

    @Test
    void doFilter_clearsMdcEvenWhenChainThrows() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class, () -> {
            org.mockito.Mockito.doThrow(new java.io.IOException("boom")).when(chain).doFilter(request, response);
            filter.doFilter(request, response, chain);
        });

        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID)).isNull();
    }
}

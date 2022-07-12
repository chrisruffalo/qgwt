package io.github.chrisruffalo.qgwt.runtime;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@WebFilter
public class CodeServerProxy implements Filter {

    public static final String BIND_ADDRESS_PARAM = "gwt-codeserver-bindadress";
    public static final String BIND_PORT_PARAM = "gwt-codeserver-bindport";

    private static final Logger LOGGER = Logger.getLogger(CodeServerProxy.class);

    private String bindAddress = "127.0.0.1";
    private int bindPort = 9876;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.bindAddress = filterConfig.getInitParameter(BIND_ADDRESS_PARAM);
        if (this.bindAddress.equals("0.0.0.0")) { // if binding to all, connect to local host
            this.bindAddress = "127.0.0.1";
        }
        try {
            final String bindPortParam = filterConfig.getInitParameter(BIND_PORT_PARAM);
            this.bindPort = Integer.parseInt(bindPortParam);
        } catch (NumberFormatException nfe) {
            // no op
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // todo: might want to filter out anything in the resources directory

        // attempt proxy on get requests only
        if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            final String method = httpServletRequest.getMethod();
            if ("GET".equalsIgnoreCase(method)) {
                // proxy get request
                try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    final String proxyPath = String.format("http://%s:%d%s", this.bindAddress, this.bindPort, httpServletRequest.getRequestURI());
                    LOGGER.tracef("Proxying GET request to %s", proxyPath);
                    final  HttpGet get = new HttpGet(proxyPath);
                    try (final CloseableHttpResponse response = httpClient.execute(get)) {
                        // only return results for a successful response that has content
                        if (200 == response.getStatusLine().getStatusCode() && response.getEntity().getContentLength() > 0) {
                            // return the status
                            httpServletResponse.setStatus(response.getStatusLine().getStatusCode());
                            // copy headers
                            Arrays.stream(response.getAllHeaders()).forEach(header -> {
                                httpServletResponse.setHeader(header.getName(), header.getValue());
                            });
                            // write body
                            response.getEntity().writeTo(httpServletResponse.getOutputStream());
                            // done, no more filter chain
                            return;
                        }
                    }
                }
            }
        }

        // if no result, continue chain
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}

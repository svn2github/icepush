/*
 * Copyright 2004-2014 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepush.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

public class CustomHeaderFilter implements Filter {

    private FilterConfig filterConfig = null;

    public void init(FilterConfig filterConfig)
            throws ServletException {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
        this.filterConfig = null;
    }

    public void doFilter(ServletRequest req, ServletResponse resp, 
            FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResp = (HttpServletResponse) resp;
        HttpServletRequest httpRequest = (HttpServletRequest) req;

        Enumeration headerNames = filterConfig.getInitParameterNames();
        while (headerNames.hasMoreElements())  {
            String headerName = (String) headerNames.nextElement();
            String headerValue = filterConfig.getInitParameter(headerName);
            if ("Access-Control-Allow-Origin".equalsIgnoreCase(headerName)
                && "*".equals(headerValue))  {
                String origin = httpRequest.getHeader("Origin");
                httpResp.addHeader("Access-Control-Allow-Origin", 
                        origin);
            } else {
                httpResp.addHeader(headerName, headerValue);
            }
        }

        String method = httpRequest.getMethod();

        //hack to not pass OPTIONS requests to ICEpush
        if ("OPTIONS".equalsIgnoreCase(method))  {
            return;
        }

        chain.doFilter(req, resp);
    }
}


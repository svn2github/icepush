/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 */

package org.icepush.servlet;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequest;

import org.icepush.Configuration;
import org.icepush.http.ResponseHandler;
import org.icepush.http.Server;

public class ThreadBlockingAdaptingServlet implements PseudoServlet {
    private static final Logger LOG = Logger.getLogger(ThreadBlockingAdaptingServlet.class.getName());
    private static final int TIMEOUT = 600; // seconds

    private Server server;
    private Configuration configuration;

    public ThreadBlockingAdaptingServlet(final Server server, final Configuration configuration) {
        this.server = server;
        this.configuration = configuration;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        ThreadBlockingRequestResponse requestResponse = new ThreadBlockingRequestResponse(request, response, configuration);
        server.service(requestResponse);
        requestResponse.blockUntilRespond();
    }

    public void shutdown() {
        server.shutdown();
    }

    private class ThreadBlockingRequestResponse extends ServletRequestResponse {
        private final Semaphore semaphore;

        public ThreadBlockingRequestResponse(final HttpServletRequest request, final HttpServletResponse response, final Configuration configuration) throws Exception {
            super(request, response, configuration);
            semaphore = new Semaphore(1);
            //Acquire semaphore hoping to have it released by a call to respondWith() method.
            semaphore.acquire();
        }

        public void respondWith(final ResponseHandler handler) throws Exception {
            try {
                super.respondWith(handler);
            } finally {
                semaphore.release();
            }
        }

        public void blockUntilRespond() throws InterruptedException {
            //Block thread by trying to acquire the semaphore a second time.
            boolean acquired = semaphore.tryAcquire(TIMEOUT, TimeUnit.SECONDS);
            if (acquired) {
                //Release the semaphore previously acquired.
                semaphore.release();
            } else {
                String remoteAddress = "unknown";
                if( request instanceof ServletRequest ){
                    remoteAddress = ((ServletRequest)request).getRemoteAddr();
                }
                LOG.warning("No response sent to " +
                        "request '" + request.getRequestURI() + "' " +
                        "with ICEfaces ID '" +
                        request.getParameter("ice.session") + "' " +
                        "from " + remoteAddress + " " +
                        "in " + TIMEOUT + " minutes.  " +
                        "Unblocking " +
                        "thread '" + Thread.currentThread().getName() + "'.");
                //Release the semaphore; most probably respondWith() method was not invoked.
                semaphore.release();
            }
        }
    }
}

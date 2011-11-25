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

package org.icepush;

import org.icepush.http.Request;
import org.icepush.http.ResponseHandler;
import org.icepush.http.Server;
import org.icepush.http.standard.RequestProxy;

public class ParkIDsServer implements Server {
    private PushGroupManager pushGroupManager;
    private Server server;

    public ParkIDsServer(PushGroupManager pushGroupManager, Server server) {
        this.pushGroupManager = pushGroupManager;
        this.server = server;
    }

    public void service(Request request) throws Exception {
        server.service(new RequestProxy(request) {
            public void respondWith(ResponseHandler handler) throws Exception {
                String notifyBackHeader = request.getHeader("ice.notifyBack");
                String parkIdsHeader = request.getHeader("ice.parkids");
                String[] pushIds = request.getParameterAsStrings("ice.pushid");

                super.respondWith(handler);

                if (parkIdsHeader != null && !"".equals(notifyBackHeader)) {
                    pushGroupManager.park(pushIds, notifyBackHeader);
                }
            }
        });
    }

    public void shutdown() {
        server.shutdown();
    }
}
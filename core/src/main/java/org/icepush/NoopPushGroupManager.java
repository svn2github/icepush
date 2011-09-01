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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Observer;

public class NoopPushGroupManager implements PushGroupManager {
    public final static PushGroupManager Instance = new NoopPushGroupManager();

    private NoopPushGroupManager() {
    }

    public void addMember(String groupName, String pushId) {
    }

    public void addObserver(Observer observer) {
    }

    public void addPushGroupListener(PushGroupListener listener) {
    }

    public void deleteObserver(Observer observer) {
    }

    public String[] getPendingNotifications() {
        return new String[0];
    }

    public Map<String, String[]> getGroupMap() {
        return Collections.EMPTY_MAP;
    }

    public void notifyObservers(List pushIdList) {
    }

    public void push(String groupName) {
    }

    public void push(String groupName, PushConfiguration config) {
    }

    public void park(String[] pushIds, String notifyBackURI) {
    }

    public void removeMember(String groupName, String pushId) {
    }

    public void removePushGroupListener(PushGroupListener listener) {
    }

    public void shutdown() {
    }
}

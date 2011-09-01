/*
 *
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
 *
 */

package org.icepush.jsp.tags;

import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.icepush.PushContext;
import org.icepush.notify.Notifier;
import org.icepush.notify.GroupNotifier;

public class BaseTag extends TagSupport {

	protected String group;
	protected String notifier;
	protected String pushid;

	public int doStartTag() throws JspException {

		// Get a push id;
		final PushContext pc = PushContext.getInstance(pageContext
				.getServletContext());
		if (pc == null) {
			throw (new JspException(
					"PushContext not available in BaseTag.doStartTag()"));
		}
		pushid = pc.createPushId((HttpServletRequest) pageContext.getRequest(),
				(HttpServletResponse) (pageContext.getResponse()));

		// Find the notifier bean;
		Notifier notifierBean = null;
		if (notifier != null) {
			notifierBean = (Notifier) pageContext.findAttribute(notifier);
			if (notifierBean != null) {
				notifierBean.setPushContext(pc);
			} else {
				throw (new JspException("Could not find notifier bean "
						+ notifier));
			}
		}

		// Set group if there is one;
		if (group == null) {
			group = pushid;		
		}
		pc.addGroupMember(group, pushid);
		if (notifierBean != null) {
			try {
				// Set group in notifier;
				GroupNotifier gnotifier = (GroupNotifier) notifierBean;
				gnotifier.setGroup(group);
			} catch (ClassCastException e) {
			}
		}

		return SKIP_BODY;
	}

	public void release() {
		group = null;
		notifier = null;
		pushid = null;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String grp) {
		this.group = grp;
	}

	public String getNotifier() {
		return notifier;
	}

	public void setNotifier(String notifier) {
		this.notifier = notifier;
	}
}

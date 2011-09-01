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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

public class RegisterTag extends BaseTag {

	private String callback;

	public int doStartTag() throws JspException {
		int i = super.doStartTag();

		try {
			// Get the writer object for output.
			JspWriter w = pageContext.getOut();

			// Write script to register;
			w.write("<script type=\"text/javascript\">");
			w.write("ice.push.register(['" + pushid + "']," + callback + ");");
			w.write("</script>");

		} catch (IOException e) {
			e.printStackTrace();
		}

		release();
		return SKIP_BODY;
	}

	public void release() {
		super.release();
		callback = null;
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String cb) {
		this.callback = cb;
	}
}

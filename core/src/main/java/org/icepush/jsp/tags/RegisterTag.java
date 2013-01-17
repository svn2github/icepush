/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
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

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
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;

public class RegionTag extends BaseTag {

	private String page;
	private String id;
	private boolean evalJS = true;

	public int doStartTag() throws JspException {
		int i = super.doStartTag();
		String id = getId();
		try {
			// Get the writer object for output.
			JspWriter w = pageContext.getOut();

			// Write script to register;
			if (id == null) {
				id = pushid;
			}
			w.write("<script type=\"text/javascript\">");
			w.write("ice.push.register(['" + pushid + "'], function(){\n");
			w.write("ice.push.get('"
					+ ((HttpServletRequest) pageContext.getRequest())
							.getContextPath() + page
					+ "', function(parameter) { \n parameter('group', '" + group
					+ "');} , ");
			w.write("function(statusCode, responseText) {\n");
			w.write("var container = document.getElementById('" + id + "');\n");
			w.write("if( container ) container.innerHTML = responseText;");
			if( evalJS ){
				w.write("if( container ) ice.push.searchAndEvaluateScripts(container);");
			}			
			w.write("});});");
			w.write("</script>");

			// Write the div;
			w.write("<div id=\"" + id + "\">");
			w.flush();

			// Include the page;
			try {
				String params = new String("group=" + group);
				pageContext.getServletContext().getRequestDispatcher(
						page + ( page.indexOf("?") > -1 ? "&" : "?" ) + params).include(pageContext.getRequest(),
						pageContext.getResponse());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (ServletException se) {
				se.printStackTrace();
			}

			// Close the div;
			w.write("</div>");

		} catch (IOException e) {
			e.printStackTrace();
		}
		release();
		return SKIP_BODY;
	}

	public void release() {
		super.release();
		setId(null);
		page = null;
		evalJS = true;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}
	
	public void setEvalJS(boolean eval){
		this.evalJS = eval;
	}
	
	public boolean getEvalJS(){
		return evalJS;
	}
}

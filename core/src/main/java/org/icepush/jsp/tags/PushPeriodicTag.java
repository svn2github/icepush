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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.icepush.PushContext;
import org.icepush.jsp.timer.GroupIntervalTimer;

public class PushPeriodicTag extends TagSupport {

    protected String group;
    protected long interval;
    protected GroupIntervalTimer timer;

    public int doStartTag() throws JspException {

	// Get timer;
	timer = (GroupIntervalTimer)pageContext.getServletContext().getAttribute("ICEpushJSPtimer");
	if (timer == null) {
	    throw(new JspException("GroupIntervalTimer must be configured as ServletContextListener."));
	}
		timer.setPushContext(PushContext.getInstance(pageContext.getServletContext()));
	    
	return SKIP_BODY;
    }

    public int doEndTag() throws JspException{
	try {
	    timer.addGroup(group, interval);
	} catch (IllegalStateException e) {
	    throw new JspException(e.toString());
	}
	return EVAL_PAGE;
    }

    public void release() {
	group = null;
	timer = null;
    }
    
    public String getGroup() {
	return group;
    }
    public void setGroup(String grp) {
	this.group = grp;
	} 
    public long getInterval() {
	return interval;
    }
    public void setInterval(long interval) {
	this.interval = interval;
    }
}

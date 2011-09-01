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

package org.icepush.jsp.samples.push;

import java.util.Vector;
import java.util.List;

public class Members {
    private Vector in;
    private Vector out;
    private String nickname;

    public Members() {
	in = new Vector();
	out = new Vector();
	nickname = null;
    }
    
    public String getNickname() {
	return nickname;
    }
    public void setNickname(String nn) {
	nickname = nn;
	if (out.remove(nickname)) {
	    in.add(nickname);
	} else if (in.remove(nickname)) {
	    out.add(nickname);
	} else {
	    in.add(nickname);
	}
    }

    public List getIn() {
	return (List)in;
    }
    public List getOut() {
	return (List)out;
    }
}

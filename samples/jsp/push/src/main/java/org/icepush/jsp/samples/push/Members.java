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

<%--
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
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib prefix="icep" uri="http://www.icepush.org/icepush/jsp/icepush.tld"%>

<jsp:useBean id="windowNotifier" class="org.icepush.notify.GroupNotifier" scope="session">
</jsp:useBean>

<html>
<head>
	<title>Testing ICEpush JSP register tag</title>
	<script type="text/javascript" src="code.icepush"></script>
	<script type="text/javascript" src="register.js"></script>

</head>
<body>
	<h2>Testing icep:register.</h2>
	Window Notifications
	<div id="window-div">0</div>
	<icep:register notifier="windowNotifier" callback="function(){countWindow('window-div');}"/>
	Session Notifications
	<div id="session-div">0</div>
	<icep:register group="${pageContext.session.id}" callback="function(){countSession('session-div');}"/>
	Application Notifications
	<div id="application-div">0</div>
	<icep:register group="application" callback="function(){countApplication('application-div');}"/>

	<icep:pushPeriodic group="${windowNotifier.group}" interval="3000"/>
	<icep:pushPeriodic group="${pageContext.session.id}" interval="5000"/>
	<icep:pushPeriodic group="application" interval="7000"/>
</body>
</html>

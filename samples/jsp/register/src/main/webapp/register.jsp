<%--
 *
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

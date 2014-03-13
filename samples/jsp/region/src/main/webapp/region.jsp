<%--
  ~ Copyright 2004-2014 ICEsoft Technologies Canada Corp.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the
  ~ License. You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an "AS
  ~ IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
  ~ express or implied. See the License for the specific language
  ~ governing permissions and limitations under the License.
  --%>
<%
	response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");//HTTP 1.1
	response.setHeader("Pragma", "no-cache");//HTTP 1.0
	response.setHeader("Expires", "0");//prevents proxy caching
%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib prefix="icep" uri="http://www.icepush.org/icepush/jsp/icepush.tld"%>

<jsp:useBean id="windowNotifier" class="org.icepush.jsp.samples.region.GroupNotificationCounter" scope="session">
</jsp:useBean>

<jsp:useBean id="sessionNotifier" class="org.icepush.jsp.samples.region.GroupNotificationCounter" scope="session">
</jsp:useBean>

<jsp:useBean id="applicationNotifier" class="org.icepush.jsp.samples.region.GroupNotificationCounter" scope="application">
</jsp:useBean>
<jsp:setProperty name="windowNotifier" property="interval" value="3000"/>
<jsp:setProperty name="sessionNotifier" property="interval" value="5000"/>
<jsp:setProperty name="applicationNotifier" property="interval" value="7000"/>
<html>
<head>
	<title>Testing ICEpush JSP region tag</title>
	<script type="text/javascript" src="code.icepush"></script>
</head>
<body>
	<h2>Testing icep:region tag.</h2>
	<br/>
	Window Region
	<icep:region id="window-region" notifier="windowNotifier" page="/window.jsp"/>
	<br/>
	Session Region
	<icep:region group="${pageContext.session.id}" notifier="sessionNotifier" page="/session.jsp"/>
	<br/>
	Application Region
	<icep:region group="application" notifier="applicationNotifier" page="/application.jsp"/>
	<icep:pushPeriodic group="${windowNotifier.group}" interval="3000"/>
	<icep:pushPeriodic group="${sessionNotifier.group}" interval="5000"/>
	<icep:pushPeriodic group="${applicationNotifier.group}" interval="7000"/>
</body>
</html>

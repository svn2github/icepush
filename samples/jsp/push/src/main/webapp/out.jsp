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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="icep" uri="http://www.icepush.org/icepush/jsp/icepush.tld"%>

<jsp:useBean id="members" class="org.icepush.jsp.samples.push.Members" scope="application">
</jsp:useBean>
<jsp:setProperty name="members" property="*"/>

<html>
<head>
	<title>Testing icep:push tag</title>
	<script type="text/javascript" src="code.icepush"></script>
</head>
<body>
<h2>Testing icep:push</h2>
<h3>Welcome ${members.nickname}.</h3><br/>

<form method="post" action="in.jsp">
    <input type="hidden" name="nickname" value="${members.nickname}"/>
    <input type="submit" value="I Want Back In"/>
</form>

<table>
	<tr>
		<td><h4>IN&nbsp</h4></td>
		<td><h4>OUT</h4></td>
	</tr>
	<tr>
		<td><icep:region group="all" page="/whosIn.jsp"/></td>
		<td><icep:region group="all" page="/whosOut.jsp"/></td>
	</tr>
</table>
<icep:push group="all"/>
</body>
</html>

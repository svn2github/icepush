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

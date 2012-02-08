<%-- 
    Document   : wartend
    Created on : 26.10.2011, 15:03:30
    Author     : aljona
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>


<!DOCTYPE html>
<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="<spring:url value='/css/fluid_grid.css' />" />
        <link rel="stylesheet" type="text/css" href="<spring:url value='/css/main.css' />" /> 
        <title><spring:message code="title" /></title>
    </head>

    <body>

        <spring:url var="formUrlPrefix" value="/web/urlaubsverwaltung" />

        <%@include file="../include/header.jsp" %>

        <div id="content">

            <div class="container_12">

                <c:choose>

                    <c:when test="${noapps == true}">

                        <spring:message code="no.apps" />

                    </c:when>

                    <c:otherwise>     

                        <table class="app-tbl" cellspacing="0">
                            <tr>
                                <th>
                                    <spring:message code="staff" />
                                </th>
                                <th>
                                    <spring:message code="type" />
                                </th>
                                <th>
                                    <spring:message code="time" />
                                </th>
                                <th>
                                    <spring:message code="reason" />
                                </th>
                                <th>
                                    <spring:message code="days.vac" />
                                </th>
                                <th>
                                    <spring:message code="days.ill" />
                                </th>
                                <th>
                                    <spring:message code="state" />
                                </th>
                                <th class="td-detail">
                                    <spring:message code="table.detail" />
                                </th>

                            </tr>

                            <c:forEach items="${applications}" var="app" varStatus="loopStatus">
                                <tr class="${loopStatus.index % 2 == 0 ? 'even' : 'odd'}">
                                    <td>
                                        <c:out value="${app.person.firstName}" />&nbsp;<c:out value="${app.person.lastName}" />
                                    </td>
                                    <td>
                                        <spring:message code="${app.vacationType.vacationTypeName}"/>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${app.startDate == app.endDate}">
                                                am&nbsp;<joda:format style="M-" value="${app.startDate}"/>
                                            </c:when>
                                            <c:otherwise>
                                                <joda:format style="M-" value="${app.startDate}"/>&nbsp;-&nbsp;<joda:format style="M-" value="${app.endDate}"/>
                                            </c:otherwise>    
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:out value="${app.reason}"/>
                                    </td>
                                    <td>
                                        <c:out value="${app.days}"/>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${app.sickDays == null}">
                                                0
                                            </c:when>
                                            <c:otherwise>
                                                <c:out value="${app.sickDays}"/>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>                     
                                    <td>
                                        <spring:message code="${app.status.state}" />
                                    </td>

                                    <%-- four possible cases: --%>

                                    <%-- 0 : applications are waiting --%>
                                    <c:if test="${stateNumber == 0}">
                                        <c:set var="url" value="${formUrlPrefix}/application/${app.id}?state=0" />
                                    </c:if>        

                                    <%-- 1 : applications are allowed --%>
                                    <c:if test="${stateNumber == 1}">
                                        <c:set var="url" value="${formUrlPrefix}/application/${app.id}?state=1" />
                                    </c:if>

                                    <%-- 2 : applications are cancelled --%>
                                    <c:if test="${stateNumber == 2}">
                                        <c:set var="url" value="${formUrlPrefix}/application/${app.id}?state=2" />
                                    </c:if>

                                    <td class="td-detail"><a href="${url}"><img src="<spring:url value='/images/playlist.png' />" /></a></td>     

                                </tr>
                            </c:forEach>
                        </table>
                    </c:otherwise> 
                </c:choose>  

            </div>
        </div>            

    </body>

</html>

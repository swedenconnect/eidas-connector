<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix ="form" uri ="http://www.springframework.org/tags/form"%>
<!DOCTYPE html>

<!-- TODO: Just a placeholder -->

<html>
  <head>
    <title><spring:message code="sweid.ui.title" /></title>
    <meta name="viewport" content="width=device-width, initial-scale=2">
    
    <c:set var="contextPath" value="${pageContext.request.contextPath}" />
    
    <link rel="stylesheet" href="${contextPath}/bootstrap-3.3.4-dist/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="<c:url value='/css/authbsstyle.css' />"/>
    
  </head>
  <body>
  
    <div style="padding: 20px">
      <div id="mainContainer" class="container">
        <table>
          <tr>
            <td><img height="70" src="${contextPath}/images/idpLogo.png"/></td>
            <td style="padding-left: 30px;vertical-align: bottom">
              <h2><spring:message code="sweid.ui.title" /></h2>
            </td>
          </tr>
        </table>
        
        <br/>

        <div class="panel-group">
          <div class="panel panel-primary">
            <div class="panel-heading" id="requesterHeading">
              <spring:message code="sweid.ui.loginRequest" />
            </div>
            <div class="panel-body" id="spinfo">SP info</div>
          </div>
          
          
          <div class="panel-default">
            <div class="panel-body">
              <spring:message code='sweid.ui.auth.select-user-option-text' var="selectUserOptionText" />
              
              <form:form modelAttribute="authenticationResult" action="/idp/profile/extauth/simulatedAuth" method="POST" class="form-horizontal" role="form">              
                <div class="form-group">
                  <form:select path="selectedUser" class="form-control">
                    <form:option value="NONE" label="${selectUserOptionText}" />
                    <form:options items="${staticUsers}" itemLabel="uiDisplayName" itemValue="personalIdentityNumber" />    
                  </form:select>                  
                </div>
                
                <form:errors path="*" cssClass="form-group alert alert-danger" element="div" />
                
                <input type="hidden" name="authenticationKey" value="${authenticationKey}" />
                
                <button type="submit" class="btn btn-danger" name="action" value="cancel"><spring:message code='sweid.ui.cancelBtn' /></button>
                &nbsp;&nbsp;&nbsp;&nbsp;
                <button type="submit" class="btn btn-primary" name="action" value="ok"><spring:message code='sweid.ui.loginBtn' /></button>
                
                <!-- 
                <input class="btn btn-danger" type="submit" value="<spring:message code='sweid.ui.cancelBtn' />" />&nbsp;&nbsp;&nbsp;&nbsp;
                <input class="btn btn-primary" type="submit" value="<spring:message code='sweid.ui.loginBtn' />" />
                -->
              </form:form>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div id="testuserDiv"></div>
    </body>
    
</html>
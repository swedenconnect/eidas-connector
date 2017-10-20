<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix ="form" uri ="http://www.springframework.org/tags/form"%>
<!DOCTYPE html>

<html>
  <head>
    <jsp:include page="html-head.jsp" />
    
    <link rel="stylesheet" type="text/css" href="<c:url value='/css/idp-style.css' />" />
    
    <title><spring:message code="connector.ui.title" /></title>
    
  </head>
  <body>
    <div class="container">
      <div class="panel panel-default" style="margin-top: 20px">
        <div class="panel-heading" style="padding-top: 0px;padding-bottom: 0px">
          <jsp:include page="header.jsp">
            <jsp:param value="connector.ui.sign.subtitle" name="titleCode" />
           </jsp:include>
        </div>
        
        <div class="panel-body">
        
          <div class="panel-group">
          
            <c:if test="${not empty uiLanguages}">
            <div class="panel panel-default" style="border: 0; box-shadow: none">
              <div class='panel-body'>
                <div id="languageSelect" class="pull-right">
                  <form action="/idp/extauth/proxyauth/complete" method="POST"> 
                    <c:forEach items="${uiLanguages}" var="uiLang">
                    <span style="width: 35px; margin-left: 10px; height: 30px">
                      <input type="image" src="<c:url value='${uiLang.flagUrl}' />" alt="${uiLang.altText}" name="language" value="${uiLang.languageTag}">
                    </span>
                    </c:forEach>
                  </form>
                </div>              
              </div>
            </div>
            </c:if>
                      
            <div class="panel panel-primary">
              <div class="panel-heading">
                <spring:message code="connector.ui.sign.requester-info.title" />
              </div>
              <div class="panel-body" id="spinfo">
                <c:if test="${not empty signMessageConsent.spInfo.defaultLogoUrl}">
                  <div class="controlled-img-div">
                    <img src="<c:out value="${signMessageConsent.spInfo.defaultLogoUrl}" />" />
                  </div>
                  <br />                  
                </c:if>
                <div>
                  <p>
                    <c:out value="${signMessageConsent.spInfo.displayName}" />
                  </p>
                  <div style="color: #999999;">
                    <p>
                      <c:out value="${signMessageConsent.spInfo.description}" />
                    </p>
                  </div>
                </div>
                
              </div>
            </div>
            
            <c:if test="${not empty signMessageConsent.textMessage}">
            <div id="sigMessPanel" class="panel panel-primary">
              <div class="panel-heading">
                <spring:message code="connector.ui.sign.sign-message.title" />
              </div>
              <div id="SignMessageBody" class="panel-body" style="min-height: 120px;overflow: auto">                
                <div class="signMessageTextArea">
                  <c:out value="${signMessageConsent.textMessage}" escapeXml="false" />
                </div>
              </div>
            </div>
            </c:if>
            
            <div class="panel panel-primary">
              <div class="panel-heading" id="signerHeading">
                <spring:message code="connector.ui.sign.signing-as.title" />
              </div>
              <div class="panel-body" id="signerinfo">
                <table class="signertable">
                  <tr>
                    <td class="signerAttrName"><spring:message code="connector.ui.sign.user.name" /></td>
                    <td class="signerAttrValue"><c:out value="${signMessageConsent.userInfo.name}" /></td>
                  </tr>
                  <c:if test="${not empty signMessageConsent.userInfo.swedishId}">
                    <tr>
                      <td class="signerAttrName"><spring:message code="connector.ui.sign.user.swe-id" /></td>
                      <td class="signerAttrValue"><c:out value="${signMessageConsent.userInfo.swedishId}" /></td>
                    </tr>                  
                  </c:if>
                  <tr>
                    <td class="signerAttrName"><spring:message code="connector.ui.sign.user.int-id" /></td>
                    <td class="signerAttrValue"><c:out value="${signMessageConsent.userInfo.internationalId}" /></td>
                  </tr>
                  <tr>
                    <td class="signerAttrName"><spring:message code="connector.ui.sign.user.date-of-birth" /></td>
                    <td class="signerAttrValue"><c:out value="${signMessageConsent.userInfo.dateOfBirth}" /></td>
                  </tr>
                  <tr>
                    <td class="signerAttrName"><spring:message code="connector.ui.sign.user.country" /></td>
                    <td class="signerAttrValue"><c:out value="${signMessageConsent.userInfo.country}" /></td>
                  </tr>
                </table>
              </div>
            </div>
                        
          </div>
          
          <form class="form-horizontal" role="form" action="/idp/extauth/proxyauth/complete" method="post">
            <button type="submit" class="btn btn-default" name="action" value="cancel"><spring:message code='connector.ui.sign.button.decline' /></button>
            &nbsp;&nbsp;&nbsp;&nbsp;
            <button type="submit" class="btn btn-primary" name="action" value="ok"><spring:message code='connector.ui.sign.button.sign' /></button>            
          </form>          
          
        </div>  
      </div>
  
       <div class='panel panel-footer'>
         <div class="panel-body">
           <jsp:include page="footer.jsp"/>
         </div>
       </div>  
    </div>
  </body>
    
</html>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>

<html>

<head>
<jsp:include page="html-head2.jsp" />
<title><spring:message code="connector.ui.title" /></title>
</head>

<body>

  <div class="container-fluid header">
    <div class="container">
      <div class="row no-gutter">
        <c:choose>
          <c:when test="${not empty signMessageConsent.spInfo.defaultLogoUrl}">
            <div class="top-logo">
              <img class="top-logo-dim" src="<c:out value='${signMessageConsent.spInfo.defaultLogoUrl}' />" />
            </div>
          </c:when>
          <c:otherwise>
            <div class="top-logo">
              <img class="top-logo-dim" src="<c:url value='/img/sc-logo-temporary.svg' />" />
            </div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>

  <div class="container main">

    <div class="row">
      <div class="col-sm-12">
        <c:choose>
          <c:when test="${not empty uiLanguages}">
            <form action="/idp/extauth/proxyauth/complete" method="POST" name="languageForm">
              <c:forEach items="${uiLanguages}" var="uiLang">
                <button class="lang float-right btn btn-link" type="submit" value="${uiLang.languageTag}"
                  name="language" id="language_${uiLang.languageTag}">${uiLang.altText}</button>
              </c:forEach>
            </form>
          </c:when>
          <c:otherwise>
            <span class="lang float-right">&nbsp;</span>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
    
    <div class="row">
      <div class="col-sm-12">
        <div class="alert alert-dismissible alert-danger">
          <button type="button" class="close" data-dismiss="alert">&times;</button>
          <h4 class="alert-heading"><spring:message code="connector.ui.development.title" /></h4>
          <p class="mb-0"><spring:message code="connector.ui.development.text1" /></p>
          <p class="mb-0">
            <spring:message code="connector.ui.development.text2" />
            <a href="https://swedenconnect.se" class="alert-link">swedenconnect.se</a>.
          </p>
        </div>
      </div>
    </div>    

    <div class="row">
      <div class="col-sm-12 content-container">

        <div class="row">
          <div class="col-sm-12 content-heading">
            <h2>
              <spring:message code="connector.ui.sign.subtitle" />
            </h2>
          </div>
          <div class="col-sm-12">
            <spring:message code="connector.ui.sign.info.default-sp-name" var="defaultName" />
            <c:set var="displayName" value="${not empty signMessageConsent.spInfo.displayName ? signMessageConsent.spInfo.displayName : defaultName}" />
            <p class="info">
              <c:choose>
                <c:when test="${not empty signMessageConsent.textMessage}">
                  <spring:message code="connector.ui.sign.info" arguments="${displayName}" />
                </c:when>
                <c:otherwise>
                  <spring:message code="connector.ui.sign.info.nosigmsg" arguments="${displayName}" />
                </c:otherwise>
              </c:choose>
            </p>
          </div>
        </div>

        <c:if test="${not empty signMessageConsent.textMessage}">
          <div class="full-width sign-message">
            <div class="row no-gutters">
              <div class="col">
                <c:out value="${signMessageConsent.textMessage}" escapeXml="false" />
              </div>
            </div>
          </div>
        </c:if>

        <div class="verification">
          <div class="row">
            <div class="col-12">
              <p class="info">
                <spring:message code="connector.ui.sign.signing-as" />
              </p>
            </div>
            <div class="col-12">
              <div class="box">
                <c:if test="${not empty signMessageConsent.userInfo.name}">
                  <span class="name dont-break-out">${signMessageConsent.userInfo.name}</span>
                </c:if>
                <c:choose>
                  <c:when test="${not empty signMessageConsent.userInfo.swedishId}">
                    <span class="info-line dont-break-out">${signMessageConsent.userInfo.swedishId}</span>
                  </c:when>
                  <c:when test="${not empty signMessageConsent.userInfo.dateOfBirth}">
                    <span class="info-line dont-break-out">${signMessageConsent.userInfo.dateOfBirth}</span>
                  </c:when>
                </c:choose>
                <c:if test="${not empty signMessageConsent.userInfo.internationalId}">
                  <span class="info-line dont-break-out"><spring:message code="connector.ui.sign.user.int-id" /> ${signMessageConsent.userInfo.internationalId}</span>
                </c:if>
                <!-- 
                <span class="info-line">Sweden</span>
                -->
                
                <form class="form-horizontal" role="form" action="/idp/extauth/proxyauth/complete" method="post" name="okForm">                  
                  <button type="submit" class="btn btn-primary" name="action" value="ok" id="okButton">
                    <spring:message code='connector.ui.sign.button.sign' />
                  </button>            
                </form>                
              </div>
            </div>
          </div>
        </div> <!-- ./verification -->

      </div> <!-- ./content-container -->
      
      <div class="col-sm-12 return">
        <form class="form-horizontal" role="form" action="/idp/extauth/proxyauth/complete" method="post" name="cancelForm">
          <button type="submit" class="btn btn-link" name="action" value="cancel" id="cancelLink">
            <spring:message code='connector.ui.button.cancel-return' />
          </button>            
        </form>
      </div>
      
      <jsp:include page="footer2.jsp" />      

    </div> <!-- ./row -->

  </div> <!-- ./container main -->
  
  <jsp:include page="final-includes.jsp" />

</body>

</html>
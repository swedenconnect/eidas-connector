<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>

<html lang="<spring:message code='connector.ui.lang' text='sv' />">

<head>
<jsp:include page="html-head2.jsp" />
<title><spring:message code="connector.ui.title" /></title>
</head>

<body>

  <div class="container-fluid header">
    <div class="container">
      <div class="row no-gutter">
        <c:choose>
          <c:when test="${not empty spInfo.defaultLogoUrl}">
            <div class="top-logo">
              <c:set var="logoAltName" value="${not empty spInfo.displayName ? spInfo.displayName : ''}" />
              <img class="top-logo-dim" src="<c:out value='${spInfo.defaultLogoUrl}' />" alt="${logoAltName}" />
            </div>
          </c:when>
          <c:otherwise>
            <div class="top-logo">
              <img class="top-logo-dim" src="<c:url value='/img/sc-logo.svg' />" alt="Sweden Connect" />
            </div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
  
  <div class="container main">
  
    <jsp:include page="dev-alert.jsp" />
        
    <div class="row">
      <div class="col-sm-12">
        <c:choose>
          <c:when test="${not empty uiLanguages}">
            <form action="<%=request.getContextPath()%>/extauth/start" method="POST">
              <c:forEach items="${uiLanguages}" var="uiLang">
                <button class="lang float-right btn btn-link" type="submit" value="${uiLang.languageTag}"
                  name="language" id="language_${uiLang.languageTag}" lang="${uiLang.languageTag}">${uiLang.altText}</button>
              </c:forEach>
            </form>
          </c:when>
          <c:otherwise>
            <span class="lang float-right">&nbsp;</span>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
    
    <c:choose>
    <c:when test="${pingFlag}">
    <div class="alert alert-warning" role="alert">  
    <div class="row">
      <div class="col-sm-12" style="padding-left: 23px; padding-right: 23px; padding-top: 23px; padding-bottom: 0px; font-size: 14px; margin-bottom: 0px;">
        <span style="font-weight: bold;"><spring:message code="conector.ui.ping-msg-title" /></span>
        <p style="padding-bottom: 0px; margin-bottom: 0px;">
          <spring:message code="conector.ui.ping-msg-msg" />
        </p>
      </div>
    </div>
    </div>    
    </c:when>
    </c:choose>      
    
    <div class="row">
      <div class="col-sm-12 content-container">

        <div class="row">
          <div class="col-sm-12 content-heading">
            <h1>
              <spring:message code="connector.ui.choose-country" />
            </h1>
          </div>
          <div class="col-sm-12">
            <p class="info content-heading-text">
              <spring:message code="connector.ui.select-country.info.default-sp-name" var="defaultName" />
              <c:set var="displayName" value="${not empty spInfo.displayName ? spInfo.displayName : defaultName}" />
              <spring:message code="connector.ui.select-country.info.1" arguments="${displayName}" />
            </p>
          </div>
        </div>

        <hr class="full-width">

        <form action="<%=request.getContextPath()%>/extauth/proxyauth" method="POST" id="countrySelectForm">
        
          <c:if test="${empty countries}">
            <div class="row">
              <div class="col-sm error">                
                <p class="error-text">
                  <spring:message code='connector.ui.no-countries' />                  
                </p>
                <p class="error-text">                
                  <button type="submit" class="btn btn-primary" name="selectedCountry" value="cancel" id="cancelLink2">
                    <spring:message code='connector.ui.button.return' />
                  </button>
                </p>
              </div>
            </div>
          </c:if>
        
          <div class="tablet-up">
            <c:set var="counter" value="0" />

            <div class="row flags">

              <c:forEach items="${countries}" var="country">
                <c:set var="flag" value="${country.isRealCountry() ? country.code : 'eu'}" />
                <c:url value="/eidas-style/images/flags/${fn:toLowerCase(flag)}.png" var="flagSrc" />

                <div class="col-sm">
                  <button class="btn country-button" type="submit" name="selectedCountry" value="${country.code}" id="countryFlag_${country.code}">
                    <img class="col-sm country-flag float-left" src="${flagSrc}" alt="" />
                    <span class="w-100"></span>
                    <span class="col-sm country-name float-left">${country.name}</span>
                  </button>
                </div>

                <c:set var="counter" value="${counter + 1}" />
                <c:if test="${counter mod 5 == 0}">
                  <!-- Break columns to a new row -->
                  <div class="w-100 flags"></div>
                </c:if>

              </c:forEach>

              <c:if test="${counter mod 5 != 0}">
                <!-- Add empty columns so that the last row is 5 colums wide -->
                <c:set var="additionalDivs" value="${5 - (fn:length(countries) mod 5)}" />

                <c:forEach begin="${counter + 1}" end="${counter + additionalDivs}" step="1" varStatus="loop">
                  <div class="col-sm"></div>
                </c:forEach>
              </c:if>

            </div> <!-- /.row -->

          </div> <!-- /.tablet-up -->

          <div class="tablet-down">
            <div class="row flags">
              <c:forEach items="${countries}" var="country">
                <c:set var="flag" value="${country.isRealCountry() ? country.code : 'eu'}" />
                <c:url value="/eidas-style/images/flags/${fn:toLowerCase(flag)}.png" var="flagSrc" />

                <div class="col-3">
                  <button class="btn country-button" type="submit" name="selectedCountry" value="${country.code}" id="countryFlagSm_${country.code}">
                    <img class="col-sm country-flag float-left" src="${flagSrc}" alt="${country.name}" />
                    <span class="w-100"></span>
                    <span class="col-sm country-name float-left">${country.name}</span>
                  </button>
                </div>

              </c:forEach>
            </div>
          </div> <!-- /.tablet-down -->

        </form>
        
        <noscript>
          <dl>
            <dt><spring:message code='connector.ui.help.1.title' /></dt>
            <dd><spring:message code='connector.ui.help.1.text' /></dd>
            <dt><spring:message code='connector.ui.help.2.title' /></dt>
            <dd><spring:message code='connector.ui.help.2.text' /></dd>
          </dl>
        </noscript>
        
        <div class="row">
          <div class="col-12">
            <div class="accordion noscripthide drop-down" id="helpAccordion">
              <div class="card">  
                <div class="card-header" id="helpHeading1">
                  <h2 class="mb-0">
                    <button class="btn btn-accordion btn-block text-left" type="button"
                      data-toggle="collapse" data-target="#collapse1" aria-expanded="false"
                      aria-controls="collapse1">
                        <spring:message code='connector.ui.help.1.title' />
                        <span class="btn-accordion-arrow"></span>
                    </button>
                  </h2>
                </div> 
                <div id="collapse1" class="collapse" aria-labelledby="helpHeading1" data-parent="#helpAccordion">
                  <div class="card-body">
                    <spring:message code='connector.ui.help.1.text' />
                  </div>
                </div>
              </div> <!-- card -->
              <div class="card">  
                <div class="card-header" id="helpHeading2">
                  <h2 class="mb-0">
                    <button class="btn btn-accordion btn-block text-left" type="button"
                      data-toggle="collapse" data-target="#collapse2" aria-expanded="false"
                      aria-controls="collapse2">
                        <spring:message code='connector.ui.help.2.title' />
                        <span class="btn-accordion-arrow"></span>
                    </button>
                  </h2>
                </div> 
                <div id="collapse2" class="collapse" aria-labelledby="helpHeading2" data-parent="#helpAccordion">
                  <div class="card-body">
                    <spring:message code='connector.ui.help.2.text' />
                  </div>
                </div>
              </div> <!-- card -->
            </div>
          </div>
        </div>        
        
      </div> <!-- /.content-container -->

      <c:if test="${not empty countries}">
      <div class="col-sm-12 return">
        <form action="<%=request.getContextPath()%>/extauth/proxyauth" method="POST" id="countrySelectForm2">
          <button type="submit" class="btn btn-link" name="selectedCountry" value="cancel" id="cancelLink">
            <spring:message code='connector.ui.button.cancel-return' />
          </button>        
        </form>
      </div>
      </c:if>

      <jsp:include page="footer2.jsp" />

    </div> <!-- /.row -->
    
  </div> <!-- /.container .main -->

  <jsp:include page="final-includes.jsp" />

</body>

</html>

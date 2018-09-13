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
          <c:when test="${not empty spInfo.defaultLogoUrl}">
            <div class="top-logo">
              <img class="top-logo-dim" src="<c:out value='${spInfo.defaultLogoUrl}' />" />
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
            <form action="/idp/extauth/start" method="POST">
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
      <div class="col-sm-12 content-container">

        <div class="row">
          <div class="col-sm-12 content-heading">
            <h2>
              <spring:message code="connector.ui.choose-country" />
            </h2>
          </div>
          <div class="col-sm-12">
            <p class="info"> <!-- content-heading-text -->
              <spring:message code="connector.ui.select-country.info.default-sp-name" var="defaultName" />
              <c:set var="displayName" value="${not empty spInfo.displayName ? spInfo.displayName : defaultName}" />
              <spring:message code="connector.ui.select-country.info.1" arguments="${displayName}" />
            </p>
          </div>
        </div>

        <hr class="full-width">

        <form action="/idp/extauth/proxyauth" method="POST" id="countrySelectForm">
        
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
                    <img class="col-sm country-flag float-left" src="${flagSrc}" alt="${country.name}" />
                    <div class="w-100"></div>
                    <p class="col-sm country-name float-left">${country.name}</p>
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
                    <div class="w-100"></div>
                    <p class="col-sm country-name float-left">${country.name}</p>
                  </button>
                </div>

              </c:forEach>
            </div>
          </div> <!-- /.tablet-down -->

        </form>


        <div class="drop-down-container noscripthide">

          <div class="col-sm-12 drop-down">
            <p>
              <spring:message code='connector.ui.help.1.title' />
            </p>

            <div class="drop-down-info">
              <spring:message code='connector.ui.help.1.text' />
            </div>
          </div> <!-- /drop-down -->

          <div class="col-sm-12 drop-down">
            <p>
              <spring:message code='connector.ui.help.2.title' />
            </p>

            <div class="drop-down-info">
              <spring:message code='connector.ui.help.2.text' />
            </div>
          </div> <!-- /drop-down -->

        </div> <!-- /.drop-down-container -->
        
      </div> <!-- /.content-container -->

      <c:if test="${not empty countries}">
      <div class="col-sm-12 return">
        <form action="/idp/extauth/proxyauth" method="POST" id="countrySelectForm2">
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

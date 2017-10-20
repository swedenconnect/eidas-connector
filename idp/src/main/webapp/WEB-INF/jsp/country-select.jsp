<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix ="form" uri ="http://www.springframework.org/tags/form"%>
<!DOCTYPE html>

<html>
  <head>
    <jsp:include page="html-head.jsp" />
    
    <link rel="stylesheet" type="text/css" href="<c:url value='/js/bs-select/css/bootstrap-select.min.css' />" />
    <script type="text/javascript" src="<c:url value='/js/bs-select/js/bootstrap-select.min.js' />"></script>
    <link rel="stylesheet" type="text/css" href="<c:url value='/css/idp-style.css' />" />
        
    <title><spring:message code="connector.ui.title" /></title>
    
  </head>
  <body>
  
    <div class="container">
      <div class="panel panel-default" style="margin-top: 20px">
        <div class="panel-heading" style="padding-top: 0px;padding-bottom: 0px">
           <jsp:include page="header.jsp">
            <jsp:param value="connector.ui.subtitle" name="titleCode" />
           </jsp:include>
        </div>
        <div class="panel-body">
        
          <div class="col-sm-6 col-sm-push-6">

            <c:if test="${not empty uiLanguages}">
            <div id="languageSelect" class="pull-right">
              <form action="/idp/extauth/start" method="POST">
                <div style="min-height: 35px"> 
                  <c:forEach items="${uiLanguages}" var="uiLang">
                    <span style="width: 35px; margin-left: 10px; height: 30px">
                      <input type="image" src="<c:url value='${uiLang.flagUrl}' />" alt="${uiLang.altText}" name="language" value="${uiLang.languageTag}">
                    </span>
                  </c:forEach>
                </div>
              </form>
            </div>  
            </c:if>
             
            <div class='panel panel-default' style="border: 0; box-shadow: none">
              <div class='panel-body' style="min-height: 250px">
                <!-- <h4 style="color: #204d74"><spring:message code="connector.ui.select-country.info.title" /></h4> -->
                <c:if test="${not empty spInfo.defaultLogoUrl}">
                <div class="controlled-img-div">
                  <img src="<c:out value="${spInfo.defaultLogoUrl}" />" />
                </div>
                <br />
                </c:if>
                <div style="color: #666">
                  <spring:message code="connector.ui.select-country.info.default-sp-name" var="defaultName" />
                  <c:set var="displayName" value="${not empty spInfo.displayName ? spInfo.displayName : defaultName}" />
                   
                  <p><spring:message code="connector.ui.select-country.info.1" arguments="${displayName}" /></p>
                  <p><spring:message code="connector.ui.select-country.info.2" /></p>
                </div>
              </div>
            </div>
          </div>        
        
          <div class="col-sm-6 col-sm-pull-6">
            <form action="/idp/extauth/proxyauth" method="POST" id="formTab3">
              <div class="form-group form-group-sm">
                <label for="countryInp"><spring:message code="connector.ui.choose-country" /></label>
                <select class="form-control selectpicker" id="selectCountryInp" name="selectedCountry" data-native-menu="false">                
                  <c:forEach items="${countries}" var="country">                    
                    <c:set var="flag" value="${country.isRealCountry() ? country.code : 'EU'}" />
                    <c:url value="/img/flags/${flag}.png" var="flagSrc" />                             
                    <c:set var="selectedAttr" value="${country.code == selectedCountry ? 'selected' : ''}" />                               
                    <option value="${country.code}" data-content="<img src='${flagSrc}'>&nbsp;&nbsp;${country.name}" ${selectedAttr}>${country.name}</option>
                  </c:forEach>
                </select>
              </div>
              <button type="submit" class="btn btn-primary btn-md btn-block" name="action" value="authenticate"><spring:message code='connector.ui.button.authenticate' /></button>
              <br />
              <button type="submit" class="btn btn-default btn-sm" name="action" value="cancel"><spring:message code='connector.ui.button.cancel' /></button>
              <!-- <input type="hidden" name="authenticationKey" value="${authenticationKey}" /> -->
            </form>
          </div>
                                   
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
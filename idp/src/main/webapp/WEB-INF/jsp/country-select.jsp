<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix ="form" uri ="http://www.springframework.org/tags/form"%>
<!DOCTYPE html>

<html>
  <head>
    <jsp:include page="c-htmlHead.jsp" />
        
    <title><spring:message code="connector.ui.title" /></title>
    
    <!--<c:set var="contextPath" value="${pageContext.request.contextPath}" />-->

    <link rel="stylesheet" type="text/css" href="<c:url value='/js/bs-select/css/bootstrap-select.min.css' />" /> 
    <link rel="stylesheet" type="text/css" href="<c:url value='/css/authbsstyle.css' /> "/>
    
  </head>
  <body>
  
    <div class="container">
      <div class="panel panel-default" style="margin-top: 20px">
        <div class="panel-heading" style="padding-top: 0px;padding-bottom: 0px">
          <jsp:include page="header.jsp"/>
        </div>
        <div class="panel-body">
          <div class="col-sm-6">
            <form action="/idp/profile/extauth/proxyauth" method="POST" id="formTab3">
              <div class="form-group form-group-sm">
                <label for="countryInp"><spring:message code="connector.ui.choose-country" /></label>
                <select class="form-control selectpicker" id="selectCountryInp" name="selectedCountry" data-native-menu="false" onchange="storeCountry();">
                  <c:forEach items="{countries}" var="country">
                    <c:set var="flag" value="${country.realCountry ? country.code : 'EU'}" />
                    <option value="${country.code}" data-content="<img src='img/flags/${flag}'>&nbsp;&nbsp;${country.name}" />
                  </c:forEach>               
                </select>
              </div>
              <button id="submit_tab3" type="submit" class="btn btn-primary btn-lg btn-block" name="action" value="authenticate">Authenticate</button>
              <input type="hidden" name="authenticationKey" value="${authenticationKey}" />
            </form>
           </div>
           <div class="col-sm-6">
             <div class='panel panel-default'>
               <div class='panel-body' style="min-height: 300px">
                 <h4 style="color: #204d74">Information</h4>
                 <div style="color: #666">
                   <p><b>A Swedish service provider</b> has requested authentication of your identity using an eID from another country outside of Sweden.</p>
                   <p>In order to transfer you to the electronic identification service in your home country, you must select <b>the country where your eID was issued</b>.</p>
                 </div>
               </div>
             </div>
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
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>

      
      <div class="col-sm-12 copyright">
        <div class="row">
          <div class="col-6">
            <img class="float-left" src="<c:url value='/img/idp-logo.svg' />" height="40" alt="Sweden Connect" /> 
          </div>
          <div class="col-6">
            <p class="float-right"><spring:message code="connector.ui.copyright" /></p>
          </div>
        </div>
        <c:if test="${not empty accessibilityUrl}">
        <div class="row mt-4">
          <div class="col-12">
            <div class="d-flex justify-content-end">
              <a href="${accessibilityUrl}" id="accesability-report">
                <spring:message code="connector.ui.accessibility-link" />
              </a>
            </div>
          </div>
        </div>
        </c:if>  
      </div>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<div class="row hidden-xs">
  <div class="col-sm-4">
    <img src="<c:url value='/custom/SE/logo_notext_h67.png' />" height="20"> 
    <span style="color: #7f0927"><spring:message code="connector.ui.e-identification-board" /></span>  
  </div>
  <div class="col-sm-4" style="text-align: center">
    <a href="<spring:message code="connector.ui.e-identification-board.info.url" />">http://www.elegnamnden.se</a>
    <br/><spring:message code="connector.ui.e-identification-board.address" />  
  </div>
  <div class="col-sm-4" style="text-align: right">
    <spring:message code="connector.ui.phone" />: +46-10 574 21 00<br/>
    <spring:message code="connector.ui.e-mail" />: <a href="mailto:kansliet@elegnamnden.se">kansliet@elegnamnden.se</a>  
  </div>
</div>
<div class="visible-xs" style="text-align: center">
  <img src="<c:url value='/custom/SE/logo_notext_h67.png' />" height="20"> 
  <span style="color: #7f0927"><spring:message code="connector.ui.e-identification-board" /></span>  
  <br /><br />
  <a href="<spring:message code="connector.ui.e-identification-board.info.url" />">http://www.elegnamnden.se</a>
  <br/><spring:message code="connector.ui.e-identification-board.address" />
  <br />  
  <spring:message code="connector.ui.phone" />: +46-10 574 21 00<br/>
  <spring:message code="connector.ui.e-mail" />: <a href="mailto:kansliet@elegnamnden.se">kansliet@elegnamnden.se</a>  
</div>

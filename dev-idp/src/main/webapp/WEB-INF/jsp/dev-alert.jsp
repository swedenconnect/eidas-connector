<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>

    <div class="row">
      <div class="col-sm-12" style="padding-left: 23px; padding-right: 23px; padding-top: 23px; padding-bottom: 0px; font-size: 14px; margin-bottom: 0px;">
        <span style="font-weight: bold;"><spring:message code="connector.ui.development.title" /></span>
        <p style="padding-bottom: 0px; margin-bottom: 0px;">
          <spring:message code="connector.ui.development.text" /> <a href="https://swedenconnect.se">swedenconnect.se</a>.
        </p>
        <p style="padding-bottom: 0px; margin-bottom: 0px;">
          <spring:message code="connector.ui.development.logs" /> <a href="<%=request.getContextPath()%>/logs"><%=request.getContextPath()%>/logs</a>.
        </p>                
      </div>
    </div>
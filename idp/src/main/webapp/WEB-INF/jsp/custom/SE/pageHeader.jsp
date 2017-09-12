<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<table style="width: 100%">
  <tr style="padding: 0px; margin: 0px">
    <td>
      <a href="<spring:message code="connector.ui.e-identification-board.info.url" />">
        <img src="<c:url value='/custom/SE/logo_full.png' />" height="100" style="margin-bottom: 0px; margin-right: 10px; margin-top: 5px">
      </a>
    </td>
    <td style="padding: 0px; margin: 0px; text-align: center">
      <h1 style="margin-top: 0; padding-top: 0"><spring:message code="connector.ui.title" /></h1>
      <h4><spring:message code="connector.ui.subtitle" /></h4>
    </td>
    <td style="text-align: right"><img src="<c:url value='/custom/SE/se-eu4.png' />" height="120" style="margin-bottom: 5px"></td>
  </tr>
</table>

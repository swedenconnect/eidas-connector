<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>

<html>
  <head>
  </head>
  <body onload="document.forms[0].submit()">
    <form action="${action}" method="post">
      <input type="hidden" name="SAMLRequest" value="${SAMLRequest}"/>
      <c:if test="${not empty RelayState}" >
        <input type="hidden" name="RelayState" value="${RelayState}" />
      </c:if>                
      <input type="submit" value="Continue" />
    </form>
  </body>
</html>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>

<html>
<head>
  <meta charset="utf-8" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <meta name="description" content="eIDAS connector">
  <meta http-equiv='pragma' content='no-cache'/>
  <meta http-equiv='cache-control' content='no-cache, no-store, must-revalidate'/>
  <meta http-equiv="Expires" content="-1"/>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      
  <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/bootstrap/css/bootstrap.min.css" >
  <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/eidas-style/dist/css/main.css" >
  <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/eidas-style/dist/css/extra.css" >
</head>

<body onload="document.forms[0].submit()">

  <form action="${action}" method="post">
    <input type="hidden" name="SAMLRequest" value="${SAMLRequest}"/>
    <c:if test="${not empty RelayState}" >
      <input type="hidden" name="RelayState" value="${RelayState}" />
    </c:if>
    <noscript>      
      <div class="container-fluid header">
        <div class="container">
          <div class="row no-gutter">        
            <div class="top-logo">
              <img class="top-logo-dim" src="<%=request.getContextPath()%>/img/idp-logo.svg" />
            </div>                  
          </div>
        </div>
      </div>
      <div class="container main">
        
        <div class="row" style="padding-top: 29px;">
          <div class="col-sm-12 content-container">
            <div class="row">
              <div class="col-sm-12">
                <p class="info content-heading-text">
                  Din webbl&auml;sare har inte JavaScript p&aring;slaget. D&auml;rf&ouml;r m&aring;ste du klicka "Forts&auml;tt" nedan.  
                </p>
                <p class="info content-heading-text">
                  Your web browser does not have JavaScript enabled. Click the "Continue" button below to proceed.  
                </p>            
              </div>
            </div> <!-- /.row -->

            <hr class="full-width">
            
            <div style="padding-top: 29px;">
              <input type="submit" class="btn btn-primary" value="Forts&auml;tt / Continue"/>
            </div>
        
          </div> <!-- /.content-container -->
          
          <div class="col-sm-12 copyright">
            <p class="float-right">Copyright &copy; Sweden Connect</p>
          </div>          
          
        </div> <!-- /.row -->
        
      </div> <!-- /.container .main -->
      
    </noscript>
  </form>
</body>
</html>
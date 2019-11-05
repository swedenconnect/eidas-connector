<%@ page pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
    <head>
    
      <meta charset="utf-8">
      <meta http-equiv="X-UA-Compatible" content="IE=edge">
      <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
      <meta name="description" content="eIDAS connector">
      <meta http-equiv='pragma' content='no-cache'/>
      <meta http-equiv='cache-control' content='no-cache, no-store, must-revalidate'/>
      <meta http-equiv="Expires" content="-1"/>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      
      <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/bootstrap/css/bootstrap.min.css" >
      <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/css/open-sans-fonts.css" >
      <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/eidas-style/dist/css/main.css" >
      <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/eidas-style/dist/css/extra.css" >    
                    
      <title>eIDAS</title>
    </head>
    
    <body>
    
      <div class="container-fluid header">
        <div class="container"> 
          <div class="row no-gutter">
            <div class="top-logo">
              <img class="top-logo-dim" src="<%=request.getContextPath()%>/img/sc-logo.svg" />
            </div>
          </div>
        </div>
      </div>    
      
      <div class="container main">
      
        <div class="row" style="padding-top: 29px;">
          <div class="col-sm-12 content-container"> 

            <div class="error">
              <div class="row">
                <div class="col-12">
                  <h2>Sidan finns inte &mdash; Not found</h2>
                </div>
                <div class="col-12 error-text">
                  <p>Det finns inget inneh&aring;ll p&aring; denna adress.<p>
                  <p>There is no content at this address.</p>
                </div>                
              </div>
            </div>

          </div>

          <div class="col-sm-12 copyright">
            <p class="float-right">Copyright &copy; Sweden Connect</p>
          </div>

        </div>  <!-- /.row -->  
      </div> <!-- /.container .main -->

    </body>
         
</html>
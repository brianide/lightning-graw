<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="esc" uri="/WEB-INF/escapelib.tld"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>LightningGraw Configuration</title>
<link rel="stylesheet" href="css/bootstrap.min.css"/>
<link rel="stylesheet" href="css/app.css"/>
</head>
<body>
<div class="container graw-main-container">
	<ul class="breadcrumb">
		<li class="active">${esc:xml(model.user.username)}</li>
	</ul>
	<form class="form-horizontal well" method="get" action="">
		<fieldset>
			<legend>Select a server to configure:</legend>
			<div class="form-group">
				<div class="col-md-11 col-md-offset-1">
					<c:choose>
						<c:when test="${not empty model.servers}">
							<c:forEach items="${model.servers}" var="server">
								<div class="radio">
									<label><input name="server" value="${server.id}" type="radio"/>${esc:xml(server.name)}</label><br/>
								</div>
							</c:forEach>
						</c:when>
						<c:otherwise>
							<div class="col-md-11">
								<div class="alert alert-warning">LightningGraw doesn't seem to be active on any servers you own!</div>
							</div>
						</c:otherwise>
					</c:choose>
				</div>
			</div>
			<div class="form-group">
				<div class="col-md-6">
					<button class="btn btn-info" type="button" onclick="showAddPopup('${model.clientId}')">Add a Server</button>
				</div>
				<div class="col-md-6 text-right">
					<a href="?logout"><button class="btn btn-default" type="button">Logout</button></a>
					<button class="btn btn-primary" type="submit">Select</button>
				</div>
			</div>
		</fieldset>
	</form>
</div>
<script src="js/jquery-3.0.0.min.js"></script>
<script src="js/bootstrap.min.js"></script>
<script>
function showAddPopup(clientId) {
	window.open('https://discordapp.com/oauth2/authorize?client_id=' + clientId + '&scope=bot&permissions=0', 'Adding a Server');
}
</script>
</body>
</html>
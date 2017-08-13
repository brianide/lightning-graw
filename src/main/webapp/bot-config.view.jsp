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
<div class="container graw-config-container">
	<ul class="breadcrumb">
		<li><a href="?">${esc:xml(model.user.username)}</a></li>
		<li class="active">${esc:xml(model.server.name)}</li>
	</ul>
	<form id="config_form" class="form-horizontal well" method="post">
		<div class="form-group">
			<div class="col-md-10 col-md-offset-1">
				<div id="alert_box" class="alert hidden"></div>
			</div>
		</div>
		<ul class="nav nav-pills">
			<li class="active"><a href="#svn_options" data-toggle="pill">SVN Options</a></li>
			<li><a href="#discord_options" data-toggle="pill">Discord Options</a></li>
		</ul>
		<br/>
		<div class="tab-content">
			<div class="tab-pane active" id="svn_options">
				<div id="error_svn_url" class="form-group">
					<label class="col-lg-3 control-label" for="svn_url">Repository URL</label>
					<div class="col-lg-9">
						<input class="form-control" name="svn_url" id="svn_url" type="url" placeholder="https://www.example.com/svn/project" value="${esc:xml(model.config.repoUrl)}"/>
					</div>
				</div>
				<div id="error_svn_un" class="form-group">
					<label class="col-lg-3 control-label" for="svn_un">Username</label>
					<div class="col-lg-9">
						<input class="form-control" name="svn_un" id="svn_un" type="text" value="${esc:xml(model.config.username)}"/>
					</div>
				</div>
				<div id="error_svn_pw" class="form-group">
					<label class="col-lg-3 control-label" for="svn_pw">Password</label>
					<div class="col-lg-9">
						<input class="form-control" name="svn_pw" id="svn_pw" type="password" value=""/>
						<div class="col-lg-12">
							<p>Password field may be omitted to keep the one currently configured.</p>
							<p>Please note that the credentials for this SVN account will necessarily be stored locally. It is advisable to create an account with read-only permissions and a generated password for this purpose.</p>
						</div>
					</div>
				</div>
				<div id="error_svn_int" class="form-group">
					<label class="col-lg-3 control-label" for="svn_int">Query Interval</label>
					<div class="col-lg-9">
						<div class="input-group">
							<input class="form-control" name="svn_int" id="svn_int" type="number" value="${esc:xml(model.config.queryInterval)}"/>
							<span class="input-group-addon"><strong>seconds</strong></span>
						</div>
						<div class="col-lg-12">
							The query interval must be a minimum of 90 seconds.
						</div>
					</div>
				</div>
				<br/>
			</div>
			<div class="tab-pane" id="discord_options">
				<div id="error_bot_chan" class="form-group">
					<label class="col-lg-3 control-label" for="bot_chan">Log Channel</label>
					<div class="col-lg-9">
						<select class="form-control" name="bot_chan" id="bot_chan">
							<option ${empty model.config.logChannel ? 'selected' : ''} value="0">---</option>
							<c:forEach items="${model.channels}" var="channel">
								<option ${channel.id eq model.config.logChannel ? 'selected' : ''} value="${esc:xml(channel.id)}">#${esc:xml(channel.name)}</option>
							</c:forEach>
						</select>
						<div class="checkbox">
							<label><input name="bot_cmd" type="checkbox" ${model.config.responsive ? 'checked' : ''}/> Respond to<span class="graw-text-fixed"> !svn </span>in all channels</label>
						</div>
					</div>
				</div>
				<div id="error_bot_role" class="form-group">
					<label class="col-lg-3 control-label" for="bot_role">Maintenance Role</label>
					<div class="col-lg-9">
						<select class="form-control" name="bot_role" id="bot_role">
							<option ${empty model.config.maintenanceRole ? 'selected' : ''} value="0">---</option>
							<c:forEach items="${model.roles}" var="role">
								<option ${role.id eq model.config.maintenanceRole ? 'selected' : ''} value="${role.id}">${esc:xml(role.name)}</option>
							</c:forEach>
						</select>
					</div>
				</div>
				<div id="error_fmt_date" class="form-group">
					<label class="col-lg-3 control-label" for="fmt_date">Date Format</label>
					<div class="col-lg-9">
						<input class="form-control graw-text-fixed" name="fmt_date" id="fmt_date" type="text" value="${esc:xml(model.config.dateFormat)}"/>
						<div class="col-lg-12">
							See <a href="http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html">the Oracle SimpleDateFormat documentation</a>.
						</div>
					</div>
				</div>
				<div id="error_fmt_msg" class="form-group">
					<label class="col-lg-3 control-label" for="fmt_msg">Message Format</label>
					<div class="col-lg-9">
						<input class="form-control graw-text-fixed" name="fmt_msg" id="fmt_msg" type="text" value="${esc:xml(model.config.messageFormat)}"/>
						<div class="col-lg-12">
							<table class="table table-condensed">
								<thead>
									<tr><th>Tag</th><th>Substitution</th></tr>
								</thead>
								<tbody>
									<tr><td class="graw-text-fixed">{{date}}</td><td>The formatted timestamp of the revision.</td></tr>
									<tr><td class="graw-text-fixed">{{rnum}}</td><td>The revision number.</td></tr>
									<tr><td class="graw-text-fixed">{{auth}}</td><td>The author of the revision.</td></tr>
									<tr><td class="graw-text-fixed">{{body}}</td><td>The body-text of the commit.</td></tr>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
			<div class="form-group">
				<div class="col-md-12 text-right">
					<a href="?"><button type="button" class="btn btn-default">Cancel</button></a>
					<button type="submit" class="btn btn-primary">Apply</button>
					<input type="hidden" name="server" value="${esc:xml(model.server.id)}"/>
					<input type="hidden" name="identity" value="${esc:xml(model.csrfToken)}"/>
				</div>
			</div>
		</div>
	</form>
</div>
<script src="js/jquery-3.0.0.min.js"></script>
<script src="js/bootstrap.min.js"></script>
<script src="js/config.js"></script>
</body>
</html>
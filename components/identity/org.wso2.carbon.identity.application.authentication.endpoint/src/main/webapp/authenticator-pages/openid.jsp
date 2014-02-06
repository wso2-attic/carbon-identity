<div class="control-group">
	<label class="control-label" for="oauth_user_name">OpenID:</label>

	<div class="controls">
		<input class="input-large" type="text" id="claimed_id" name="claimed_id"
								  size='30'/>
		<input type="hidden" name="sessionDataKey" value='<%=request.getParameter("sessionDataKey")%>'/>
	</div>
</div>

<div class="form-actions">
	<input type="submit" value="<fmt:message key='login'/>" class="btn btn-primary">
</div>

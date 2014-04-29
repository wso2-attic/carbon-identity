package org.wso2.carbon.identity.sts.passive.ui.dto;


public class SessionDTO {
	//wa
	private String action;
	//wattr
	private String attributes;
	//wctx
	private String context;
	//wreply
	private String replyTo;
	//wpseudo
	private String pseudo;
	//wtrealm
	private String realm;
	//wreq
	private String request;
	//wreqptr
	private String RequestPointer;
	//wp
	private String policy;
	
	private String reqQueryString;
	
	public SessionDTO(){}
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getAttributes() {
		return attributes;
	}
	
	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}
	
	public String getContext() {
		return context;
	}
	
	public void setContext(String context) {
		this.context = context;
	}
	
	public String getReplyTo() {
		return replyTo;
	}
	
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}
	
	public String getPseudo() {
		return pseudo;
	}
	
	public void setPseudo(String pseudo) {
		this.pseudo = pseudo;
	}
	
	public String getRealm() {
		return realm;
	}
	
	public void setRealm(String realm) {
		this.realm = realm;
	}
	
	public String getRequest() {
		return request;
	}
	
	public void setRequest(String request) {
		this.request = request;
	}
	
	public String getRequestPointer() {
		return RequestPointer;
	}
	
	public void setRequestPointer(String requestPointer) {
		RequestPointer = requestPointer;
	}
	
	public String getPolicy() {
		return policy;
	}
	
	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getReqQueryString() {
		return reqQueryString;
	}

	public void setReqQueryString(String reqQueryString) {
		this.reqQueryString = reqQueryString;
	}
}

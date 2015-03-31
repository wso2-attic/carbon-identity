//package org.wso2.carbon.identity.mgt.dto;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//public class EmailConfigDTO implements Serializable {
//
//	/**
//	 * This is not needed. TODO - remove
//	 */
//	private static final long serialVersionUID = 7405886542583476294L;
//
//	private List<EmailTemplateDTO> templates;
//
//	public EmailConfigDTO() {
//		this.templates = new ArrayList<EmailTemplateDTO>();
//	}
//
//	public EmailTemplateDTO getTemplate(String templateType) {
//		EmailTemplateDTO emailTemplate = null;
//		if (templateType != null && this.templates.size() > 0) {
//
//			for (EmailTemplateDTO template : this.templates) {
//				if (template.getName().equals(templateType)) {
//					emailTemplate = template;
//				}
//			}
//
//			if (emailTemplate == null) {
//				// Throw exception
//			}
//		} else {
//			// Throw exception
//		}
//		return null;
//	}
//
//	public void setTemplate(EmailTemplateDTO emailTemplate) {
//
//		if (emailTemplate != null) {
//			Iterator<EmailTemplateDTO> iterator = this.templates.iterator();
//			for (int i = 0; iterator.hasNext(); i++) {
//				EmailTemplateDTO template = iterator.next();
//				if (template.getName().equals(emailTemplate.getName())) {
//					iterator.remove();
//					this.templates.add(emailTemplate);
//				}
//			}
//
//		}
//	}
//	
//	public List<EmailTemplateDTO> getTemplates() {
//		return templates;
//	}
//	
//	public void setTemplates(List<EmailTemplateDTO> templates) {
//		this.templates = templates;
//	}
//
//}

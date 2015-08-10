package org.wso2.carbon.identity.sso.cas.ticket;

import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

public class TicketIdGenerator {
	public static String generate(String prefix) {
		String verifiedPrefix = (prefix != null) ? prefix : "UNKOWN";
		
		return verifiedPrefix + "-" + UUIDGenerator.generateUUID().replaceAll("-", "") + "-" + ServerConfiguration.getInstance().getFirstProperty("HostName");
	}
}

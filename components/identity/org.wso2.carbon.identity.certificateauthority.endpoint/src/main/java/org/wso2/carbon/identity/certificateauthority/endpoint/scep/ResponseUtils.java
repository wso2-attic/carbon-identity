package org.wso2.carbon.identity.certificateauthority.endpoint.scep;

import javax.ws.rs.core.Response;

public class ResponseUtils {

    public static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(message)
                .build();
    }

    public static Response methodNotAllowed(String allowedTypes) {
        return Response.status(ScepConstants.METHOD_NOT_ALLOWED)
                .header("Allow", allowedTypes)
                .build();
    }

    public static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static Response serverError() {
        return Response.serverError().build();
    }
}

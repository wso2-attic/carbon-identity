package org.wso2.carbon.claim.custom.cache;

import java.io.Serializable;

/**
 * Created by Chanuka on 6/23/15 AD.
 */
public abstract class CacheKey implements Serializable {

    private static final long serialVersionUID = 1471805737633325514L;

    @Override
    public abstract boolean equals(Object otherObject);

    @Override
    public abstract int hashCode();
}
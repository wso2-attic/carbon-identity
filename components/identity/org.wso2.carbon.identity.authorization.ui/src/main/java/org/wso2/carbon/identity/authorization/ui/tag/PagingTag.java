/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authorization.ui.tag;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class PagingTag extends BodyTagSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int pageSize;
	private Object[] dataSet;
	private int pageNumber;

	private int offset;

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public Object[] getDataSet() {
		return dataSet;
	}

	public void setDataSet(Object[] dataSet) {
		this.dataSet = dataSet;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Override
	public int doAfterBody() throws JspException {
		boolean hasMore = hasContentForThePage();
		return hasMore ? EVAL_BODY_AGAIN : SKIP_BODY;
	}

	@Override
	public int doStartTag() throws JspException {
		offset = 0;
		if (dataSet != null && dataSet.length > 0) {
			double len = (double) dataSet.length;
			int numberOfPages = (int) Math.ceil(len / pageSize);
			processPageNumbers(numberOfPages);
		}
		boolean hasMore = hasContentForThePage();
		return hasMore ? EVAL_BODY_INCLUDE : SKIP_BODY;
	}

	private boolean hasContentForThePage() {
		if (dataSet != null && dataSet.length > 0) {
			int currentItemIndex = offset + (pageSize * (pageNumber - 1));
			int pageLimit = pageSize * pageNumber;

			++offset;
			if (dataSet.length > currentItemIndex && currentItemIndex < pageLimit) {
				pageContext.setAttribute("data", dataSet[currentItemIndex]);
				return true;
			}
		}
		return false;
	}

	private void processPageNumbers(int numberOfPages) {
		List<Integer> pages = new ArrayList<Integer>();
		pages.add(1);

		if (pageNumber - 1 > 1) {
			pages.add(pageNumber - 1);
		}
		if (pageNumber - 1 > 0) {
			pages.add(pageNumber);
		}

		if (pageNumber + 1 < numberOfPages) {
			pages.add(pageNumber + 1);
		}
		if (pageNumber != numberOfPages) {
			pages.add(numberOfPages);
		}

		pageContext.setAttribute("pages", pages);

	}

}

/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

function showNewRuleBox(link) {
    link.style.display = "none";
    var rowToHide = document.getElementById(link.id + "Row");
    if (rowToHide.style.display == "none") {
        rowToHide.style.display = "";
    } else {
        rowToHide.style.display = "none";
    }
}
function showHideRow(link) {
    var rowToHide = document.getElementById(link.id + "Row");
    if (rowToHide.style.display == "none") {
        rowToHide.style.display = "";
        link.className = "icon-link arrowUp";
    } else {
        rowToHide.style.display = "none";
        link.className = "icon-link arrowDown";
    }
}
function handleFocus(obj, txt) {
    if (obj.value == txt) {
        obj.value = '';
        YAHOO.util.Dom.removeClass(obj, 'defaultText');

    }
}
function handleBlur(obj, txt) {
    if (obj.value == '') {
        obj.value = txt;
        YAHOO.util.Dom.addClass(obj, 'defaultText');
    }
}
YAHOO.util.Event.onDOMReady(
    function () {
        /*if (document.getElementById("resourceNamesTarget").value == "") {
         document.getElementById("resourceNamesTarget").value = "Pick resource name";
         }
         if (document.getElementById("subjectNamesTarget").value == "") {
         document.getElementById("subjectNamesTarget").value = "Pick role name";
         }
         if (document.getElementById("userAttributeValueTarget").value == "") {
         document.getElementById("userAttributeValueTarget").value = "User attribute";
         }
         if (document.getElementById("actionNamesTarget").value == "") {
         document.getElementById("actionNamesTarget").value = "Action";
         }*/
    }
);
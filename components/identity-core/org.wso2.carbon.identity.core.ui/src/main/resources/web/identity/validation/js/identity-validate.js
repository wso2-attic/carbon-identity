/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This file includes javascript functions that can be used for form input validation.
 * This file expects dialog/js/dialog.js to be included while including this script.
 */

/**
 * Returns the regular expression for the provided pattern key.
 * If no regex is defined for the provided input returns the provided input as a regex
 *
 * @param pattern regex pattern key or undefined regex as a string
 */
function getPattern(pattern) {
    var regex;
    switch (pattern) {
        case "digits-only":
            regex = /^[0-9]+/;
            break;
        case "alphabetic-only":
            regex = /^[a-zA-Z]+/;
            break;
        case "alphanumerics-only":
            regex = /^[a-zA-Z0-9]+/;
            break;
        case "url":
            regex = /^(([^:/?#]+):)?(([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?/;
            break;
        case "email":
            regex = /^(.+)@(.+)$/;
            break;
        case "whitespace-exists":
            regex = /.*\s+.*/;
            break;
        case "uri-reserved-exists":
            regex = /.*[:/\?#\[\]@!\$&'\(\)\*\+,;=]+.*/;
            break;
        case "uri-unsafe-exists":
            regex = /.*[<>%\{\}\|\^~\[\]`]+.*/;
            break;
        case "html-meta-exists":
            regex = /.*[&<>"'/]+.*/;
            break;
        case "xml-meta-exists":
            regex = /.*[&<>"']+.*/;
            break;
        default:
            regex = new RegExp(pattern);
            break;
    }

    return regex;
}

function isWhiteListed(input, whiteListPatterns) {
    var isValid = false;
    var pattern;
    for (var i = 0; i < whiteListPatterns.length; i++) {
        pattern = getPattern(whiteListPatterns[i]);
        isValid = pattern.test(input);
        if (isValid) {
            break;
        }
    }

    return isValid;
}

function isNotBlackListed(input, blackListPatterns) {
    var isValid = false;
    var pattern;
    for (var i = 0; i < blackListPatterns.length; i++) {
        pattern = getPattern(blackListPatterns[i]);
        isValid = !pattern.test(input);

        if (!isValid) {
            break;
        }
    }

    return isValid;
}

function getPatternString(patterns) {
    var patternString = "";
    for (var i = 0; i < patterns.length; i++) {
        patternString += getPattern(patterns[i]).toString();
        if ((patterns.length - 1) != i) {
            patternString += ", ";
        }
    }

    return patternString;
}

/**
 * Validates all input elements of the form which defines attributes for white list or black list patterns against them.
 *
 * @param form Form to be validated
 * @returns {{isValid: boolean, label: string, whiteListPatterns: string, blackListPatterns: string}}
 */
function validate(form) {
    var allInputs = form.getElementsByTagName('input');
    var len = allInputs.length;

    var whiteListPatternString = "";
    var blackListPatternString = "";
    var labelString = "";

    for (var i = 0; i < len; i++) {
        var element = allInputs[i];
        var value = element.value;
        if (value != null && value != 'null' && value != "") {
            var whiteListPatterns = element.getAttribute('white-list-patterns');
            var blackListPatterns = element.getAttribute('black-list-patterns');

            if ((whiteListPatterns === null || whiteListPatterns === "") &&
                (blackListPatterns === null || blackListPatterns === "")) {
                continue;
            }

            var isValid = false;
            var isWhiteListed = false;
            var isNotBlackListed = false;
            var whiteListPatternsProvided = false;
            var blackListPatternsProvided = false;

            if (whiteListPatterns != null && whiteListPatterns != "") {
                whiteListPatternsProvided = true;
                var patternArray = whiteListPatterns.split(' ');
                isWhiteListed = isWhiteListed(value, patternArray);
                whiteListPatternString = getPatternString(patternArray);
            }

            if (blackListPatterns != null && blackListPatterns != "") {
                blackListPatternsProvided = true;
                var patternArray = blackListPatterns.split(' ');
                isNotBlackListed = isNotBlackListed(value, patternArray);
                blackListPatternString = getPatternString(patternArray);
            }

            if (whiteListPatternsProvided && blackListPatternsProvided) {
                isValid = isWhiteListed || isNotBlackListed;
            } else if (whiteListPatternsProvided) {
                isValid = isWhiteListed;
            } else if (blackListPatternsProvided) {
                isValid = isNotBlackListed;
            }

            if (isValid === true) {
                continue;
            } else {
                labelString = element.getAttribute('label');
                if (labelString == null || labelString == "") {
                    labelString = element.getAttribute('name');
                }
                return {
                    isValid: false,
                    label: labelString,
                    whiteListPatterns: whiteListPatternString,
                    blackListPatterns: blackListPatternString
                };
            }
        } else {
            continue;
        }
    }

    return {
        isValid: true,
        label: labelString,
        whiteListPatterns: whiteListPatternString,
        blackListPatterns: blackListPatternString
    };
}

/**
 * Validates all input elements of the form which defines attributes for white list or black list patterns against them,
 * and returns true if all inputs are valid and pops up an error message if inputs are invalid.
 *
 * @param form
 * @param msg Message to be popped up if validations fails. Message should contain {0}, {1} and {2},
 * which are replaced by the input label, white list patterns and black list patterns respectively.
 * @returns {boolean} true if successfully validated
 */
function doValidateForm(form, msg) {
    var validationObj = validate(form);
    if (validationObj['isValid'] === true) {
        return true;
    }

    var label = validationObj['label'];
    var whiteListPatterns = validationObj['whiteListPatterns'];
    var blackListPatterns = validationObj['blackListPatterns'];

    var message = msg.replace('{0}', label).replace('{1}', whiteListPatterns === "" ? 'NONE' : whiteListPatterns)
        .replace('{2}', blackListPatterns === "" ? 'NONE' : blackListPatterns);

    CARBON.showErrorDialog(message);
    return false;

}
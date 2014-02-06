/**
 * Make an ajax call to the given url
 * 
 * @param {Object}
 *            url
 * @param {Object}
 *            method POST, GET etc
 * @param {Object}
 *            parameters request parameters to be sent
 * @param {Object}
 *            resposeType mime type of the respose
 * @param {Object}
 *            callback calback method, will be called on success
 * @param {Object}
 *            extraParameters, if paramters should be send to the callback
 *            method, those parameters should be provided as an array
 */
function makeAjax(url, method, parameters, resposeType, callback,
		extraParameters) {
	$.ajax({
		url : url,
		type : method,
		data : parameters,
		dataType : resposeType,
		success : function(data) {
			callback(data, extraParameters);
		},
		error : function() {

		}

	});
}

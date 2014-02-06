function submitAjax(obj) {
	var params = "";
	$("form[id='" + obj.attr("id") + "'] input[type='text']").each(function() {
		params += $(this).attr("name") + "=" + $(this).val() + "&";
	});

	$("form[id='" + obj.attr("id") + "'] input[type='checkbox']:checked").each(
			function() {
				params += $(this).attr("name") + "=" + $(this).val() + "&";
			});

	$("form[id='" + obj.attr("id") + "'] input[type='hidden']").each(
			function() {
				params += $(this).attr("name") + "=" + $(this).val() + "&";
			});

	var url = obj.attr("action");
	makeAjax(url, "POST", params, "json", callbackTest);
}

function callbackTest(data) {
	alert(data);
}

$(document).ready(function() {
	$("a[id*='addMorePermissions']").live("click", function() {
		addPemrissionsRow($(this));
	})

	$("a[id*='removeThisPermissions']").live("click", function() {
		removePemrissionRow($(this));
	});

	$("a[id*='removeThisAction_']").live("click", function() {
		removeActionRow($(this));
	});

	$("a[id*='addMoreAction_']").live("click", function() {
		addMoreActionRow($(this));
	});

	$("a[id*='addMoreResource_']").live("click", function(){
		addMoreResourceRow($(this));
	});
	
	$("input[type='checkbox'][id='selectAll']").live("click", function() {
		selectAll($(this));
	});

	$("#submitNewModule").live("click", function() {

		if (validatNewModuleForm()) {
			$("#moduleNewForm").submit();
		}
	});
	displayErrorMsgs();
});

function validatNewModuleForm(){
	var moduleName = $("input[name='moduleName']").val();
	if(moduleName == undefined || moduleName.length == 0){
		CARBON
		.showErrorDialog("Please enter an application name.");
		return false
	}
	var status = true;
	$("input[name^='newAction_']").each(function(){
		var val = $(this).val();
		if(val == undefined || val.length == 0){
			CARBON
			.showErrorDialog("Please enter a authorized action.");
			status = false;
		}
	});
	
	return status;
}

function submitPermissionsSearchReq() {
	var role = $("input[name='role']").val();
	var resource = $("input[name='resource']").val();

	if ((resource !== null && resource.length > 0)
			&& (role === null || role.length == 0)) {
		CARBON
				.showErrorDialog("Please provide a role before proceed with search.");
	} else {
		$("#viewPermissionsForm").submit();
	}
}

function displayErrorMsgs() {
	var errorType = $("#errorType").val();
	var msg = $("#msg").val();

	if (errorType === "error") {
		// display error
		CARBON.showErrorDialog(msg);
	} else if (errorType === "info") {
		// display info
		CARBON.showInfoDialog(msg);
	}
}

function deleteModule() {
	// TODO delete popup should be given.
}

function deletePermissions() {
	var param = "";
	var selectedLen = 0;
	$("input[type='checkbox'][id^='select_']:checked").each(function() {
		selectedLen += 1;
		param += $(this).val() + ",";
	});
	if (selectedLen == 0) {
		CARBON
				.showErrorDialog("Please select atleast one permission to delete");
	}

	$("#operation").val(4);
	$("#deleted").val(param);
	$("#permissionsEditForm").submit();
}

function deleteAction(moduleId, action) {

}

function finishAdding() {

	var emptyFound = false;
	$(
			"input[id^='permResource'], input[id^='permAction'], input[id^='permRole']")
			.each(function() {
				var val = $(this).val();
				if (val === null || val.length == 0) {
					emptyFound = true;
				}
			});
	if (emptyFound) {
		CARBON
				.showErrorDialog("Please enter values to the empty 'Resource, Action and Role' fields before submitting");
	} else {
		$("#addForm").submit();
	}
}

function savePermissionCallback(data, extraParam) {
	alert(data);
	if (data.status === "success") {
		// display the success message
		location.href = "/carbon/identity-authorization/index.jsp";
	} else {
		// display the emoveThisAction_error message
	}
}

function cancellAdding() {
	location.href = "/carbon/identity-authorization/index.jsp?region=region1&item=authorization_menu";
}

function createDependancySections(jqObj){
}


function addMoreResourceRow(obj) {
	var count = $("#numberOfResources");
	var previousCount = parseInt(count.val(), 10);

	var newIndex = previousCount + 1;

	count.val(newIndex);

	var cloned = $("tr[id='newResourceRow_0']").clone();
	cloned.attr("id", "newResourceRow_" + newIndex);
	$("#buttonPanel_resource").before(cloned);
	cloned.show();

	var checkBox = $("tr[id^='newResourceRow_" + newIndex
			+ "'] input[name^='deleteResourcection_']");
	var inputField = $("tr[id^='newResourceRow_" + newIndex
			+ "'] input[name^='newResource_']");

	checkBox.attr("name", "deleteNewResource_" + newIndex);
	inputField.attr("name", "newResource_" + newIndex);
	inputField.val("");
}


function addMoreActionRow(obj) {
	var count = $("#numberOfActions");
	var previousCount = parseInt(count.val(), 10);

	var newIndex = previousCount + 1;

	count.val(newIndex);

	var cloned = $("tr[id='newActionRow_0']").clone();
	cloned.attr("id", "newActionRow_" + newIndex);
	$("#buttonPanel_action").before(cloned);
	cloned.show();

	var checkBox = $("tr[id^='newActionRow_" + newIndex
			+ "'] input[name^='deleteNewAction_']");
	var inputField = $("tr[id^='newActionRow_" + newIndex
			+ "'] input[name^='newAction_']");

	checkBox.attr("name", "deleteNewAction_" + newIndex);
	inputField.attr("name", "newAction_" + newIndex);
	inputField.val("");
}

function removePemrissionRow(obj) {
	var index = parseInt(obj.attr("id").split("_")[1], 10);
	var row = $("#permRow_" + index);

	var prevIndex = index - 1;
	if (prevIndex >= 1) {

		var addLink = $(
				"tr[id='permRow_" + index + "'] a[id^='addMorePermissions_'] ")
				.clone();
		var removeLink = $(
				"tr[id='permRow_" + index
						+ "'] a[id^='removeThisPermissions_'] ").clone();

		addLink.attr("id", "addMorePermissions_" + prevIndex);
		removeLink.attr("id", "removeThisPermissions_" + prevIndex);

		var prevLinkTd = $("#permRow_" + prevIndex + " td[id^='permAddMore_']");
		prevLinkTd.append(addLink);
		prevLinkTd.append(removeLink);

		if (prevIndex > 1) {
			removeLink.show();
		} else {
			removeLink.hide();
		}

		row.remove();
	}
}

function addPemrissionsRow(obj) {
	var index = parseInt(obj.attr("id").split("_")[1], 10);
	var row = $("#permRow_" + index);

	var newIndex = index + 1;
	var cloned = row.clone();
	cloned.attr("id", "permRow_" + newIndex)
	$("#buttonPanel").before(cloned);

	$("tr[id='permRow_" + index + "'] a[id^='addMorePermissions_'] ").remove();
	$("tr[id='permRow_" + index + "'] a[id^='removeThisPermissions_'] ")
			.remove();

	$("tr[id='permRow_" + newIndex + "'] [id^='perm'] ").each(function() {
		var current = $(this);
		var currentId = current.attr("id").split("_")[0];
		current.attr("id", currentId + "_" + newIndex);
		current.attr("name", currentId + "_" + newIndex);
		current.val("");

	});

	$("tr[id='permRow_" + newIndex + "'] a[id^='addMorePermissions_'] ").attr(
			"id", "addMorePermissions_" + newIndex);
	$("tr[id='permRow_" + newIndex + "'] td[id^='permAddMore_'] ").attr("id",
			"permAddMore_" + newIndex);

	var removeLink = $("tr[id='permRow_" + newIndex
			+ "'] a[id^='removeThisPermissions_'] ");
	removeLink.attr("id", "removeThisPermissions_" + newIndex);
	removeLink.show();

}

function selectAll(obj) {
	var clicked = obj.attr("checked");
	if (clicked) {
		// Select all
		$("input[type='checkbox'][id^='select_']").attr('checked', 'checked');

	} else {
		// Deselect all
		$("input[type='checkbox'][id^='select_']").removeAttr('checked');
	}
}
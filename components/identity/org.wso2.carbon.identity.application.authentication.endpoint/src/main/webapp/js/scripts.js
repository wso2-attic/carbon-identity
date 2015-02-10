$(document).ready(function(){
    $('#authorizeLink').click(function(){
        $('#loginForm').show('slow');
    });
	$('#loginBtn').click(function(){
			var error = "";
			if($('#oauth_user_name').val() == ""){
				error += '<div>Username field is empty.</div>';
			}
			if($('#oauth_user_password').val() == ""){
				error += '<div>Password field is empty.</div>';
			}
			if(error == ""){
				$('#errorMsg').hide('slow');
				$('#loginForm').submit();
				
			}else{				
				$('#errorMsg').html(error).show('slow');
			}
	});
	$('#denyLink').click(function(){
			$('#denyForm').submit();
	});
});


function requestTOTPToken(){

        var endpointURL = "../../commonauth";
        $.ajax({
            url:endpointURL,
            type:"GET",
            data:"&sessionDataKey="+document.getElementById("sessionDataKey").value+"&sendToken=true",
            success: function(response){
                if(response==""){
                    alert("Token sent");
                }else{
                    alert("Error");
                }
                console.log(response);
            },
            error: function(request, error){
                console.log("Error when generating a token ");
            }
        });

}

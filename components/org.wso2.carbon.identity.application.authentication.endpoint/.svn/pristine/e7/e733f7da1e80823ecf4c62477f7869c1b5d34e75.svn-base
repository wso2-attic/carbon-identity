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

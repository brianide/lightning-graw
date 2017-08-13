$('#config_form').submit(function(event) {
	
	// Stop the default form submission handling 
	event.preventDefault();
	
	$('#alert_box').addClass('hidden');
	
	// Submit the form
	$.post({
		url: 'validate',
		data: $('#config_form').serialize(),
		dataType: 'json',
		success: function(data) {
			// Clear the form status
			$('.has-error').removeClass('has-error');
			$('#alert_box').removeClass('hidden alert-success alert-warning alert-danger');
			$('#alert_box').html(data.message);
			
			// Update the form with the error status
			if(data.status === 'success') {
				$('#alert_box').addClass('alert-success');
			}
			else if(data.status === 'warning') {
				$('#alert_box').addClass('alert-warning');
			}
			else if(data.status === 'error') {
				$('#alert_box').addClass('alert-danger');
				$.each(data.errors, function(index, field) {
					$('#error_' + field).addClass('has-error');
				});
			}
			
			window.location.hash = "alert_box";
		}
	});
	
});
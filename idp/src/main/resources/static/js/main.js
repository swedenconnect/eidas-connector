$(document).ready(function() {

	$('.noscripthide').show();


	// Not to be used in production
	$('#headertoggle').click(function() {
		$('.header').toggleClass('hide');
	});

	$('#elemtoggle').click(function() {
		$('.ns-providers').toggleClass('hide');
	});
	
});
App.CertsRoute = CloudOSProtectedRoute.extend({
	model: function () {
		return Api.find_ssl_certs();
	}
});

App.CertsRoute = Ember.Route.extend({
	model: function () {
		return Api.find_ssl_certs();
	}
});

App.CertRoute = Ember.Route.extend({
	model: function (params) {
		var cert = Api.find_ssl_cert(params['cert_name']);
		return  cert;
	},
});

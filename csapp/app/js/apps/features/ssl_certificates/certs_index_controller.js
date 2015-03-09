App.CertsIndexController = Ember.ObjectController.extend({
	actions: {
		doRemoveCert: function (name) {
			if (!Api.remove_ssl_cert(name)) {
				alert('error removing cert: '+name);
			}
		}
	}
});

App.CertsNewController = Ember.ObjectController.extend({
	actions: {
		doAddCert: function () {
			var name = this.get('certName');
			var description = this.get('description');
			var pem = this.get('pem');
			var key = this.get('key');
			var cert = {
				'name': name,
				'description': description,
				'pem': pem,
				'key': key
			};
			if (!Api.add_ssl_cert(cert)) {
				alert('error adding cert: '+name);
			}
		}
	}
});

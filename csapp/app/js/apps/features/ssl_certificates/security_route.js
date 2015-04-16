App.SecurityRoute = CloudOSProtectedRoute.extend({
	setupController: function(controller, model) {
		this.transitionTo('certs.index');
	}
});

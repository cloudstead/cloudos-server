App.SecurityRoute = Ember.Route.extend({
	setupController: function(controller, model) {
		this.transitionTo('certs.index');
	}
});

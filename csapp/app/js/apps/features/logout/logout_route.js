App.LogoutRoute = Ember.Route.extend({
	setupController: function(controller, model) {
		CloudOs.logout();
		window.location.replace('/index.html');
		return;
	}
});

App.ProfileRoute = Ember.Route.extend({
	model: function() {
		return App.Profile.findByName(CloudOs.account().name);
	},
	setupController: function(controller, model) {
		controller.set('model', model);
		controller.set('original_model', model.copy());
	}
});

App.ValetKeysRoute = Ember.Route.extend({
	model: function (params) {
		return Api.get_service_keys();
	},
	setupController: function(controller, model) {
		this._super(controller, model);
		controller.set("isShhAllowed", Api.check_allow_ssh().value === "true" ? true : false);
	}
});

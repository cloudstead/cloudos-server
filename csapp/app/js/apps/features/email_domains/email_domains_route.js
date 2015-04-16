App.EmailDomainsRoute = CloudOSProtectedRoute.extend({
	model: function () {
		return Api.list_email_domains();
	},

	setupController: function(controller, model) {
		this._super(controller, model);
		controller.set('configuration', Api.get_system_configuration())
	},

	actions: {
		refreshContent: function(controller) {
			controller.set("content", Api.list_email_domains());
		}
	}
});

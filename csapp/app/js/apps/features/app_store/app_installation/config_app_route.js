App.ConfigAppRoute = App.AppstoreConfigBasicRoute.extend({

	setupController: function(controller, model) {
		this._super(controller, model);
		controller.set("config", this.get("config"));
		controller.set("app", this.get("app"));
	},

	actions: {
		transitionToConfirm: function() {
			this.transitionTo("confirm_config_app", this.get("app.name"));
		}
	}
});

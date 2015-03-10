App.ConfirmConfigAppRoute = App.AppstoreConfigBasicRoute.extend({

	setupController: function(controller, model) {
		this._super(controller, model);
		controller.set("config", this.get("config"));
		controller.set("app", this.get("app"));
	},

	actions:{
		transitionToConfig: function() {
			this.transitionTo("config_app", this.get("app.name"));
		},
		transitionToInstall: function() {
			this.transitionTo("install_app", this.get("app.name"));
		}
	}
});
